package com.yiqun.nav.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * @Description : 声明式页面生命周期监听钩子
 * @param onAppear 页面上屏/可见时触发 (ON_RESUME)
 * @param onDisappear 页面被覆盖/退居后台时触发 (ON_PAUSE)
 * @param onDispose 页面被 Pop 出栈销毁时触发
 * @Author : Dalingge
 * @Time :2026/7/30  18:19
 */
@Composable
fun rememberScreenLifecycle(
    onAppear: () -> Unit = {},
    onDisappear: () -> Unit = {},
    onDispose: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnAppear by rememberUpdatedState(onAppear)
    val currentOnDisappear by rememberUpdatedState(onDisappear)
    val currentOnDispose by rememberUpdatedState(onDispose)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentOnAppear()
                Lifecycle.Event.ON_PAUSE -> currentOnDisappear()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentOnDispose() // 页面出栈彻底销毁
        }
    }
}