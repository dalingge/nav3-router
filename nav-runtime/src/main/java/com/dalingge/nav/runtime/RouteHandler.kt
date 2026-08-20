package com.dalingge.nav.runtime

import android.net.Uri

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/3  10:32
 */

interface RouteHandler {
    fun handle(uri: Uri): Boolean
}