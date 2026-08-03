package com.yiqun.nav.runtime.handler

import android.net.Uri
import com.yiqun.nav.runtime.NavCenter
import com.yiqun.nav.runtime.RouteHandler
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/31  10:44
 */
class WebViewHandler(private val webViewRoute: String, private val domains: Set<String> = setOf()) : RouteHandler {


    override fun handle(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: ""

        // 校验域名是否在白名单中 (支持完全匹配 & *.domain.com 子域名匹配)
        val isWhitelisted = domains.any { whitelistedDomain ->
            host == whitelistedDomain || host.endsWith(".$whitelistedDomain")
        }

        if (setOf("http", "https").contains(uri.scheme) && isWhitelisted) {
            val encodedUrl = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
            val targetWebViewUrl = "$webViewRoute?url=$encodedUrl"
            NavCenter.navigate(targetWebViewUrl)
            return true
        }
        return false
    }
}