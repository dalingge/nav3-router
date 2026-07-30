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

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/30  10:35
 */
@Composable
@Screen(route = "app/user", needLogin = true)
fun UserScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("用户页 (User)", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            NavCenter.popWithResult("result_key", "Result from User 达令哥")
        }) {
            Text("带结果 Pop 返回")
        }
    }
}