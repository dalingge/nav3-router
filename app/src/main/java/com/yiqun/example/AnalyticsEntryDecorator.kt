package com.yiqun.example

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntryDecorator
import com.yiqun.nav.runtime.NavDestination

/**
 *
 * @Description : 示例：自动给每一个进入屏幕的页面做全埋点曝光统计
 * @Author : Dalingge
 * @Time :2026/7/30  14:07
 */
class AnalyticsEntryDecorator : NavEntryDecorator<NavDestination>(
    // 当页面出栈且离开 Composition 时触发，进行状态清理
    onPop = { contentKey ->
        println("📊 [页面 Pop 出栈销毁] 彻底清理 contentKey = $contentKey 的缓存数据")
    },
    // 渲染 UI
    decorate = { entry ->
        val destination = entry.metadata["destination"] as? NavDestination

        LaunchedEffect(destination) {
            if (destination != null) {
                println("📊 [页面曝光] route = ${destination.route}")
            }
        }

        // 核心：调用官方提供的 entry.Content() 渲染页面！
        entry.Content()
    }
)