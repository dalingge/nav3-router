package com.dalingge.nav.runtime

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 *
 * @Description : 管理多 Tab 独立栈状态
 * @Author : Dalingge
 * @Time :2026/7/31  09:21
 */
class TabNavigatorState(
    val stacks: Map<String, NavStack>,
    initialTab: String,
) {
    var currentTab by mutableStateOf(initialTab)
        private set

    fun switchTab(tabKey: String) {
        if (stacks.containsKey(tabKey)) {
            currentTab = tabKey
        }
    }

    fun currentStack(): NavStack? = stacks[currentTab]
}

@Composable
fun rememberTabNavigatorState(
    vararg tabs: String,
    initialTab: String = tabs.first(),
): TabNavigatorState {
    return remember {
        val map = tabs.associateWith { tabKey -> NavStack(tabKey) }
        TabNavigatorState(map, initialTab)
    }
}

/**
 * 多 Tab 独立栈渲染容器
 */
@Composable
fun TabNavHost(
    state: TabNavigatorState,
    modifier: Modifier = Modifier,
) {
    val activeStack = state.currentStack() ?: return
    Box(modifier = modifier) {
        // 渲染当前 Tab 的专属独立导航栈
        NavHostContainer(stack = activeStack)
    }
}