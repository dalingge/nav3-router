package com.yiqun.nav.annotation

import kotlin.reflect.KClass

/**
 *
 * @Description :标记 Compose 页面函数
 * @Author : Dalingge
 * @Time :2026/7/28  14:43
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Screen(
    val route: String,
    val needLogin: Boolean = false,
    val interceptors: Array<KClass<*>> = [],
    val enterTransition: KClass<*> = UnspecifiedTransition::class
)
