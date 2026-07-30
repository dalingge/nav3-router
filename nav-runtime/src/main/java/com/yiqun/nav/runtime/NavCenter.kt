package com.yiqun.nav.runtime

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.yiqun.nav.annotation.InterceptResult
import com.yiqun.nav.annotation.RouteInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

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
    private val scope = CoroutineScope(Dispatchers.Main)
    val primaryStack = NavStack("GlobalPrimary")
    private val resultStore = mutableStateMapOf<String, Any?>()
    /** 全局默认转场动画 */
    private var globalTransition: NavTransition = DefaultSlideTransition()

    /** 在 Activity / Application 中全局设置转场动画 */
    fun setDefaultTransition(transition: NavTransition): NavCenter {
        this.globalTransition = transition
        return this
    }

    fun getGlobalTransition(): NavTransition = globalTransition

    /** 业务层全局拦截器链 */
    private val globalInterceptors = mutableListOf<RouteInterceptor>()

    fun addGlobalInterceptor(interceptor: RouteInterceptor): NavCenter {
        globalInterceptors.add(interceptor)
        return this
    }

    // 支持存储 @Composable 组合函数闭包
    private val decoratorFactories = mutableListOf<@Composable () -> NavEntryDecorator<NavDestination>>()

    /** 链式注册普通静态 NavEntryDecorator 实例 */
    fun addEntryDecorator(decorator: NavEntryDecorator<NavDestination>): NavCenter {
        decoratorFactories.add { decorator }
        return this
    }

    /** 链式注册 @Composable 工厂闭包 (专门支持官方 rememberViewModelStoreNavEntryDecorator) */
    fun addEntryDecorator(factory: @Composable () -> NavEntryDecorator<NavDestination>): NavCenter {
        decoratorFactories.add(factory)
        return this
    }

    /** 在 Render 时动态求值获取所有 Decorator 实例 */
    @Composable
    fun getDecorators(): List<NavEntryDecorator<NavDestination>> {
        return decoratorFactories.map { it.invoke() }
    }

    override fun navigate(destination: NavDestination, builder: (NavOptionsBuilder.() -> Unit)?): NavCenter {
       return navigate(destination.toUrl(), builder)
    }

    override fun navigate(url: String, builder: (NavOptionsBuilder.() -> Unit)?) : NavCenter{
        val options = NavOptionsBuilder().apply(builder ?: {}).build()

        scope.launch {
            val uri = url.toUri()
            val path = uri.path?.removePrefix("/") ?: uri.schemeSpecificPart
            val meta = NavRegistry.getMeta(path) ?: return@launch

            // 全局拦截器
            for (interceptor in globalInterceptors) {
                when (val result = interceptor.intercept(url)) {
                    is InterceptResult.Proceed -> continue
                    is InterceptResult.Redirect -> {
                        navigate(result.targetUrl, builder)
                        return@launch
                    }

                    is InterceptResult.Abort -> return@launch
                }
            }

            // 路由私有拦截器
            for (interceptor in meta.interceptors) {
                when (val result = interceptor.intercept(url)) {
                    is InterceptResult.Proceed -> continue
                    is InterceptResult.Redirect -> {
                        navigate(result.targetUrl, builder)
                        return@launch
                    }

                    is InterceptResult.Abort -> return@launch
                }
            }

            val queryParams = uri.queryParameterNames.associateWith { name ->
                uri.getQueryParameter(name) ?: ""
            }
            val destination = meta.factory(queryParams)

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

            if (options.launchSingleTop && backstack.lastOrNull()?.route == meta.route) {
                return@launch
            }

            backstack.add(destination)
        }

        return this
    }

    override fun pop(): Boolean {
        val backstack = primaryStack.backstack
        if (backstack.size > 1) {
            backstack.removeAt(backstack.lastIndex)
            return true
        }
        return false
    }

    override fun <T> popWithResult(key: String, result: T) {
        resultStore[key] = result
        pop()
    }

    @Composable
    fun <T> getResult(key: String): State<T?> {
        val state = remember { mutableStateOf<T?>(null) }
        LaunchedEffect(resultStore[key]) {
            @Suppress("UNCHECKED_CAST")
            val valResult = resultStore[key] as? T
            if (valResult != null) {
                state.value = valResult
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
    val allDecorators = listOf(saveableDecorator) + NavCenter.getDecorators()

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
                },
            )
        }
    }


}