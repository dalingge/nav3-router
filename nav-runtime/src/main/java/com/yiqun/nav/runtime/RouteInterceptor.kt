package com.yiqun.nav.runtime

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/3  10:33
 */

/**
 * 路由拦截器接口（归属于 runtime 运行时层）
 */
interface RouteInterceptor {
    fun intercept(url: String): InterceptResult
}

/**
 * 拦截结果密封类
 */
sealed class InterceptResult {
    object Proceed : InterceptResult()
    data class Redirect(val targetUrl: String) : InterceptResult()
    object Abort : InterceptResult()
}