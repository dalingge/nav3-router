package com.dalingge.nav.runtime

import android.content.Intent


/**
 * Intent 路由解析策略接口（解耦策略模式）
 */
interface IntentResolver {
    /** 从 Intent 中解析出目标路由 URL，解析失败返回 null */
    fun resolve(intent: Intent?): String?
}

/**
 * 默认 Intent 解析器：支持标准 Scheme URI 与指定 Extra Key
 */
class DefaultIntentResolver(
    private val extraKey: String = "NAV_TARGET_URL"
) : IntentResolver {

    override fun resolve(intent: Intent?): String? {
        if (intent == null) return null

        //  优先解析标准的 Scheme URI (如 myapp://shop/detail?id=10086 或 https://...)
        val dataUrl = intent.dataString
        if (!dataUrl.isNullOrEmpty()) {
            return dataUrl
        }

        //  备选解析 Extra 参数 (如推送通知 Bundle 包含的 NAV_TARGET_URL 字段)
        val extraUrl = intent.getStringExtra(extraKey)
        if (!extraUrl.isNullOrEmpty()) {
            return extraUrl
        }

        return null
    }
}