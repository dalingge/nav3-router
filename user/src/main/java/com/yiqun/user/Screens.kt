package com.yiqun.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yiqun.nav.annotation.Screen
import com.yiqun.nav.runtime.NavCenter
import com.yiqun.nav.runtime.rememberScreenLifecycle

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/30  10:35
 */
@Composable
@Screen(route = "app/user", needLogin = true)
fun UserScreen() {

    // 声明式监听页面显隐与出栈销毁
    rememberScreenLifecycle(
        onAppear = { println("📊 [DetailScreen] 页面上屏/可见") },
        onDisappear = { println("📊 [DetailScreen] 页面被覆盖/退居后台") },
        onDispose = { println("📊 [DetailScreen] 页面已被 Pop 出栈销毁") }
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("用户页 (User)", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            NavCenter.popWithResult("result_key", "Result from User 达令哥")
        }) {
            Text("带结果 Pop 返回")
        }
    }
}