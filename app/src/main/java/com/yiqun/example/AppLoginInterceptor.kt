package com.yiqun.example

import com.yiqun.nav.runtime.NavRegistry
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.net.toUri
import com.yiqun.nav.runtime.InterceptResult
import com.yiqun.nav.runtime.RouteInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/28  16:50
 */
/** App 业务层维护的用户会话 */
object UserSession {
    var isLoggedIn: Boolean = false
}

/**
 * App 业务层自定义登录拦截器：完全由业务决定拦截规则！
 */
class AppLoginInterceptor : RouteInterceptor {
    override suspend fun intercept(url: String): InterceptResult {
        val uri = url.toUri()
        val path = uri.path?.removePrefix("/") ?: uri.schemeSpecificPart
        val meta = NavRegistry.getMeta(path)

        // 校验页面元数据是否配置了 needLogin = true
        if (meta?.needLogin == true && !UserSession.isLoggedIn) {
            val encodedTarget = withContext(Dispatchers.IO) {
                URLEncoder.encode(url, StandardCharsets.UTF_8.name())
            }
            // 业务自定义重定向目标
            return InterceptResult.Redirect("app/login?redirect=$encodedTarget")
        }

        return InterceptResult.Proceed
    }
}