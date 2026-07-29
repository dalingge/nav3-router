package com.yiqun.nav.annotation

import kotlin.reflect.KClass

/**
 *
 * @Description :定义路由注解、拦截器与转场动画合约。
 * @Author : Dalingge
 * @Time :2026/7/28  14:43
 */


/**
 * 标记 Compose 页面函数
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Screen(
    val route: String,
    val needLogin: Boolean = false,
    val interceptors: Array<KClass<out RouteInterceptor>> = [],
    val enterTransition: KClass<*> = UnspecifiedTransition::class
)


/** 标记未配置局部动画（降级使用全局动画） */
class UnspecifiedTransition


/**
 * 拦截器接口与结果
 */
interface RouteInterceptor {
    suspend fun intercept(url: String): InterceptResult
}

sealed class InterceptResult {
    object Proceed : InterceptResult()
    data class Redirect(val targetUrl: String) : InterceptResult()
    object Abort : InterceptResult()
}