package com.dalingge.nav.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 *
 * @Description :服务发现管理器
 * @Author : Dalingge
 * @Time :2026/8/21  09:59
 */


/**
 * 服务接口标记
 */
interface IService


/**
 * 高性能、线程安全的无 UI 跨模块服务注册中心（0 反射）
 */
object NavServiceRegistry {
    // 按接口 Class 映射
    private val servicesByClass = ConcurrentHashMap<Class<*>, Any>()
    // 按路径 Path 映射
    private val servicesByPath = ConcurrentHashMap<String, Any>()

    fun <T : Any> register(contract: Class<T>, instance: T, path: String = "") {
        servicesByClass[contract] = instance
        if (path.isNotEmpty()) {
            servicesByPath[path] = instance
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(contract: Class<T>): T? = servicesByClass[contract] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getByPath(path: String): T? = servicesByPath[path] as? T
}