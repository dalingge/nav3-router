package com.dalingge.nav.runtime

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/29  11:38
 */

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * 极简共享元素绑定 Modifier
 * @param key 共享元素的唯一标识 (如 "avatar_10086")
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementKey(key: Any): Modifier {

    val isInInspection = LocalInspectionMode.current
    if (isInInspection){
        return this
    }

    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedContentScope.current

    return if (sharedScope != null) {
        with(sharedScope) {
            this@sharedElementKey.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope,
                renderInOverlayDuringTransition = true
            )
        }
    } else {
        this
    }
}