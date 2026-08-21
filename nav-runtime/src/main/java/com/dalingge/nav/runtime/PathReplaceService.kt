package com.dalingge.nav.runtime

/**
 *
 * @Description :动态路径重写服务
 *               动态路径/URL 重写策略接口（用于 A/B 测试、动态域名/路径映射
 * @Author : Dalingge
 * @Time :2026/8/21  10:00
 */
interface PathReplaceService {
    /** 传入原始 URL，返回重写后的目标 URL */
    fun replace(rawUrl: String): String
}