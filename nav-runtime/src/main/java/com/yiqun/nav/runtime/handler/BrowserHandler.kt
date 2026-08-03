package com.yiqun.nav.runtime.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yiqun.nav.runtime.RouteHandler

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/31  11:09
 */
class BrowserHandler(private val context: Context) : RouteHandler {

    override fun handle(uri: Uri): Boolean {
        if (setOf("http", "https").contains(uri.scheme)) {
            openExternalBrowser(context, uri)
            return true
        }
        return false
    }


    /** 拉起外部系统浏览器 */
    private fun openExternalBrowser(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}