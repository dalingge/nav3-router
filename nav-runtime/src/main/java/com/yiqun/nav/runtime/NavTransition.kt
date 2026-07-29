package com.yiqun.nav.runtime

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset


/**
 * 页面转场动画接口
 */
interface NavTransition {
    fun pushEnter(): EnterTransition
    fun pushExit(): ExitTransition
    fun popEnter(): EnterTransition
    fun popExit(): ExitTransition
}

/**
 * 经典水平滑动转场动画
 */
class DefaultSlideTransition(
    durationMillis: Int = 300,
) : NavTransition {

    private val animSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis)

    override fun pushEnter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { it }, // 从最右侧滑入
        animationSpec = animSpec
    ) + fadeIn()

    override fun pushExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { -it }, // 向最左侧滑出
        animationSpec = animSpec
    ) + fadeOut()

    override fun popEnter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = animSpec,
    ) + fadeIn()

    override fun popExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = animSpec,
    ) + fadeOut()
}

/** 底部弹窗转场（Pop 时向下滑出） */
class BottomSheetTransition : NavTransition {
    override fun pushEnter(): EnterTransition = slideInVertically { it } + fadeIn()
    override fun pushExit(): ExitTransition = fadeOut()
    override fun popEnter(): EnterTransition = fadeIn()
    override fun popExit(): ExitTransition = slideOutVertically { it } + fadeOut()
}

/**
 * 专用于共享元素页面的转场动画（容器淡入淡出，把位移完全交给共享元素）
 */
class SharedElementTransition(
    private val durationMillis: Int = 300
) : NavTransition {
    override fun pushEnter(): EnterTransition = fadeIn(tween(durationMillis))
    override fun pushExit(): ExitTransition = fadeOut(tween(durationMillis))
    override fun popEnter(): EnterTransition = fadeIn(tween(durationMillis))
    override fun popExit(): ExitTransition = fadeOut(tween(durationMillis))
}
