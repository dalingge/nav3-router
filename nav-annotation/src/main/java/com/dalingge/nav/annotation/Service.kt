package com.dalingge.nav.annotation

import kotlin.reflect.KClass

/**
 *
 * @Description : 跨模块服务暴露注解（标注在服务实现类上）
 * @param contract 服务对外暴露的接口 Class
 * @param path 服务的可选组/路径标识（可选，支持按 Path 查找服务）
 * @Author : Dalingge
 * @Time :2026/8/21  09:58
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service(
    val contract: KClass<*>,
    val path: String = ""
)
