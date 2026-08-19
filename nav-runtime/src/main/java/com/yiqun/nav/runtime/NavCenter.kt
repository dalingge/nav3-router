package com.yiqun.nav.runtime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 *
 * @Description :全局导航中心（上层双轨路由 + 拦截器 + 结果回传）
 * @Author : Dalingge
 * @Time :2026/7/28  13:53
 */

class NavStack(val name: String = "Main") {
    val backstack: SnapshotStateList<NavDestination> = mutableStateListOf()
}

object NavRegistry {
    private val registry = mutableMapOf<String, RouteMeta>()
    fun register(meta: RouteMeta) {
        registry[meta.route] = meta
    }

    fun getMeta(route: String): RouteMeta? = registry[route]
}

object NavCenter : Navigator {
    val primaryStack = NavStack("GlobalPrimary")

    private val resultStore = mutableStateMapOf<String, Any?>()

    //  统一使用 CopyOnWriteArrayList 确保 100% 线程安全
    private val routeHandlers = CopyOnWriteArrayList<RouteHandler>()
    private val globalInterceptors = CopyOnWriteArrayList<RouteInterceptor>()
    private val decoratorFactories = CopyOnWriteArrayList<@Composable () -> NavEntryDecorator<NavDestination>>()

    @Volatile
    private var globalTransition: NavTransition = DefaultSlideTransition()
    @Volatile
    private var fallbackRoute: String? = null
    // 注入解耦的 Intent 解析策略，默认使用 DefaultIntentResolver
    @Volatile
    private var intentResolver: IntentResolver = DefaultIntentResolver()

    // 主线程同步重入锁
    private val navLock = ReentrantLock()

    /**  自定义 Intent 解析策略（如复杂加密推送 Payload） */
    fun setIntentResolver(resolver: IntentResolver): NavCenter {
        this.intentResolver = resolver
        return this
    }

    /** 解耦接管系统 Intent / Scheme / 推送通知跳转 */
    fun handleIntent(intent:  Intent?, builder: (NavOptionsBuilder.() -> Unit)? = null): Boolean {
        val targetUrl = intentResolver.resolve(intent)
        if (!targetUrl.isNullOrEmpty()) {
            navigate(targetUrl, builder)
            return true
        }
        return false
    }

    /** 将当前 Backstack 序列化存入 Bundle（进程被杀前调用） */
    fun saveState(outState: Bundle): Bundle {
        return navLock.withLock {
            val urlList = primaryStack.backstack.map { it.toUrl() }
            outState.putStringArrayList("NAV3_SAVED_BACKSTACK_${primaryStack.name}", ArrayList(urlList))
            outState
        }
    }

    /** 从 Bundle 中恢复被杀死前的整个页面 Backstack（进程重开后调用） */
    fun restoreState(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState == null) return false
        return navLock.withLock {
            val savedUrls = savedInstanceState.getStringArrayList("NAV3_SAVED_BACKSTACK_${primaryStack.name}")
            if (!savedUrls.isNullOrEmpty()) {
                primaryStack.backstack.clear()
                savedUrls.forEach { url ->
                    // 依次重建并恢复栈中的每一个页面！
                    navigate(url)
                }
                true
            } else {
                false
            }
        }
    }

    fun setFallbackRoute(route: String): NavCenter {
        this.fallbackRoute = route
        return this
    }

    fun addRouteHandler(handler: RouteHandler): NavCenter {
        this.routeHandlers.add(handler)
        return this
    }

    fun addEntryDecorator(decorator: NavEntryDecorator<NavDestination>): NavCenter {
        decoratorFactories.add { decorator }
        return this
    }

    fun addEntryDecorator(factory: @Composable () -> NavEntryDecorator<NavDestination>): NavCenter {
        decoratorFactories.add(factory)
        return this
    }

    @Composable
    fun getEntryDecorators(): List<NavEntryDecorator<NavDestination>> {
        return decoratorFactories.map { it.invoke() }
    }

    fun setDefaultTransition(transition: NavTransition): NavCenter {
        this.globalTransition = transition
        return this
    }

    fun getGlobalTransition(): NavTransition = globalTransition

    fun addGlobalInterceptor(interceptor: RouteInterceptor): NavCenter {
        this.globalInterceptors.add(interceptor)
        return this
    }

    override fun navigate(destination: NavDestination, builder: (NavOptionsBuilder.() -> Unit)?): NavCenter {
        navigate(destination.toUrl(), builder)
        return this
    }

    override fun navigate(url: String, builder: (NavOptionsBuilder.() -> Unit)?): NavCenter {
        val options = NavOptionsBuilder().apply(builder ?: {}).build()

        // 🟢 主线程绝对同步且原子化执行，零 runBlocking，零 ANR 风险
        navLock.withLock {
            var currentUrl = url
            var redirectCount = 0
            val maxRedirects = 10
            var targetMeta: RouteMeta? = null
            var finalUri: Uri? = null

            // 同步 Redirect 循环
            while (redirectCount <= maxRedirects) {
                val uri = Uri.parse(currentUrl)
                finalUri = uri

                //  责任链前置处理
                var handled = false
                for (handler in routeHandlers) {
                    if (handler.handle(uri)) {
                        handled = true
                        break
                    }
                }
                if (handled) return@withLock

                //  匹配路由元数据
                val path = uri.path?.removePrefix("/") ?: uri.schemeSpecificPart
                var meta = NavRegistry.getMeta(path)

                // 404 容错降级
                if (meta == null && !fallbackRoute.isNullOrEmpty() && path != fallbackRoute) {
                    meta = NavRegistry.getMeta(fallbackRoute!!)
                }

                if (meta == null) {
                    // 若路由找不到，提前跳出 while 循环走统一日志输出
                    break
                }

                //  全局拦截器链 (纯同步极其迅速)
                var hasRedirect = false
                for (interceptor in globalInterceptors) {
                    when (val result = interceptor.intercept(currentUrl)) {
                        is InterceptResult.Proceed -> continue
                        is InterceptResult.Redirect -> {
                            currentUrl = result.targetUrl
                            hasRedirect = true
                            redirectCount++
                            break
                        }
                        is InterceptResult.Abort -> return@withLock
                    }
                }
                if (hasRedirect) continue

                //  路由私有拦截器链 (纯同步)
                for (interceptor in meta.interceptors) {
                    when (val result = interceptor.intercept(currentUrl)) {
                        is InterceptResult.Proceed -> continue
                        is InterceptResult.Redirect -> {
                            currentUrl = result.targetUrl
                            hasRedirect = true
                            redirectCount++
                            break
                        }
                        is InterceptResult.Abort -> return@withLock
                    }
                }
                if (hasRedirect) continue

                targetMeta = meta
                break
            }

            // 修正：在 while 循环外精准判断并输出超限或未找到日志
            if (targetMeta == null || finalUri == null) {
                if (redirectCount > maxRedirects) {
                    Log.e("NavCenter", "❌ Redirect chain exceeded $maxRedirects hops, last url: $currentUrl")
                } else {
                    val path = finalUri?.let { it.path?.removePrefix("/") ?: it.schemeSpecificPart } ?: ""
                    Log.w("NavCenter", "⚠️ Route not found for path: '$path' (url=$currentUrl)")
                }
                return@withLock
            }

            val queryParams = finalUri.queryParameterNames.associateWith { name ->
                finalUri.getQueryParameter(name) ?: ""
            }

            val destination = try {
                targetMeta.factory(queryParams)
            } catch (e: Exception) {
                Log.e("NavCenter", "❌ Failed to instantiate destination for route: ${targetMeta.route}", e)
                return@withLock
            }

            //  栈控制策略
            val backstack = primaryStack.backstack
            if (options.clearTask) {
                backstack.clear()
            } else if (options.popUpToRoute != null) {
                val index = backstack.indexOfLast { it.route == options.popUpToRoute }
                if (index != -1) {
                    val targetIndex = if (options.inclusive) index else index + 1
                    while (backstack.size > targetIndex) {
                        backstack.removeAt(backstack.lastIndex)
                    }
                }
            }

            if (options.launchSingleTop && backstack.lastOrNull()?.route == targetMeta.route) {
                return@withLock
            }

            backstack.add(destination)
        }

        return this
    }

    override fun pop(): Boolean {
        return navLock.withLock {
            val backstack = primaryStack.backstack
            if (backstack.size > 1) {
                backstack.removeAt(backstack.lastIndex)
                true
            } else {
                false
            }
        }
    }

    override fun <T> popWithResult(key: String, result: T): Boolean {
        return navLock.withLock {
            val backstack = primaryStack.backstack
            if (backstack.size > 1) {
                resultStore[key] = result
                backstack.removeAt(backstack.lastIndex)
                true
            } else {
                false
            }
        }
    }

    fun clearResult(key: String) {
        resultStore.remove(key)
    }

    @Composable
    fun <T> getResult(key: String): State<T?> {
        val state = remember { mutableStateOf<T?>(null) }
        LaunchedEffect(resultStore[key]) {
            if (resultStore.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                state.value = resultStore[key] as T?
                resultStore.remove(key)
            }
        }
        return state
    }

    @Composable
    fun Render() {
        NavHostContainer(stack = primaryStack)
    }
}

/**
 * 直接桥接官方 Navigation 3 的 NavDisplay 与 NavEntry
 */
@Composable
fun NavHostContainer(stack: NavStack) {
    //防止冷启动协程压栈前， NavDisplay 收到空 backstack 抛出异常
    if (stack.backstack.isEmpty()) {
        return
    }

    // 获取官方原生的 UI 状态恢复装饰器 (处理 rememberSaveable、TextField 输入框、列表滚动位置保留)
    val saveableDecorator = rememberSaveableStateHolderNavEntryDecorator<NavDestination>()
    //  官方 Saveable 装饰器 (必须置顶) + 用户链式添加的所有 Decorators
    val allDecorators = listOf(saveableDecorator) + NavCenter.getEntryDecorators()

    // 最外层包裹官方 SharedTransitionLayout
    SharedTransitionLayout {

        //  将 SharedTransitionScope 向下透传
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = stack.backstack,
                onBack = { NavCenter.pop() },
                entryDecorators = allDecorators,
                sharedTransitionScope = this,
                transitionSpec = { //使用此辅助函数定义向前导航动画
                    val transition = targetState.metadata["transition"] as? NavTransition ?: NavCenter.getGlobalTransition()
                    transition.pushEnter().togetherWith(transition.pushExit())
                },
                popTransitionSpec = {//使用此辅助函数可为特定 NavEntry 定义返回导航动画
                    val transition = targetState.metadata["transition"] as? NavTransition ?: NavCenter.getGlobalTransition()
                    transition.popEnter().togetherWith(transition.popExit())
                },
                predictivePopTransitionSpec = {
                    val transition = targetState.metadata["transition"] as? NavTransition ?: NavCenter.getGlobalTransition()
                    transition.popEnter().togetherWith(transition.popExit())
                },
                entryProvider = { destination ->
                    val meta = NavRegistry.getMeta(destination.route)
                    val transition = meta?.transition ?: NavCenter.getGlobalTransition()
                    NavEntry(
                        key = destination, metadata = mapOf(
                            "destination" to destination,
                            "transition" to transition
                        )
                    ) {
                        if (meta != null) {
                            meta.content(destination)
                        }
                    }
                }
            )
        }
    }
}