package com.dalingge.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dalingge.nav.annotation.Screen
import com.dalingge.nav.runtime.NavCenter

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/30  10:35
 */
@Composable
@Screen(route = "app/user", needLogin = true)
fun UserScreen() {


    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        println("📊 [UserScreen] 页面初始化")
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        println("📊 [UserScreen] 页面上屏/可见")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        println("📊 [UserScreen] 页面被覆盖/退居后台")
    }

    DisposableEffect(Unit) {
        onDispose {
            println("📊 [UserScreen] 页面已被 Pop 出栈销毁")
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("用户页 (User)", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            NavCenter.popWithResult("result_key", "Result from User 达令哥")
        }) {
            Text("带结果 Pop 返回")
        }
    }
}