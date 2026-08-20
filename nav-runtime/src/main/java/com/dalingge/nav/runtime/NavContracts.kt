package com.dalingge.nav.runtime

import androidx.compose.runtime.Composable
import java.util.UUID

/**
 *
 * @Description : 接口与高级选项
 * @Author : Dalingge
 * @Time :2026/7/28  13:33
 */


/**
 * 强类型 Navigation Destination
 */
interface NavDestination {
    val route: String
    val entryId: String get() = UUID.randomUUID().toString()
    fun toUrl(): String
}

/**
 * 导航选项（高级栈控制）
 * launchSingleTop：如果栈顶已经是该页面，不再重复创建。
 * popUpTo(route, inclusive)：跳转时清空指定目标之上的所有页面（例如：登录成功跳转首页，清空登录页和注册页）。
 * clearTask：清空整个栈（例如：退出登录重置到登录页
 */
data class NavOptions(
    val launchSingleTop: Boolean = false,
    val popUpToRoute: String? = null,
    val inclusive: Boolean = false,
    val clearTask: Boolean = false
)

class NavOptionsBuilder {
    var launchSingleTop: Boolean = false
    var popUpToRoute: String? = null
    var inclusive: Boolean = false
    var clearTask: Boolean = false

    fun build() = NavOptions(launchSingleTop, popUpToRoute, inclusive, clearTask)
}

/**
 * 解耦导航接口（用于 ViewModel 单元测试）
 */
interface Navigator {
    fun navigate(destination: NavDestination, builder: (NavOptionsBuilder.() -> Unit)? = null): NavCenter
    fun navigate(url: String, builder: (NavOptionsBuilder.() -> Unit)? = null): NavCenter
    fun pop(): Boolean
    fun <T> popWithResult(key: String, result: T): Boolean
}

/**
 * 路由元数据
 */
data class RouteMeta(
    val route: String,
    val needLogin: Boolean = false, // 仅作为元数据标记，不强绑定任何拦截器实现
    val factory: (Map<String, String>) -> NavDestination,
    val content: @Composable (NavDestination) -> Unit,
    val interceptors: List<RouteInterceptor> = emptyList(),
    val transition: NavTransition? = null
)