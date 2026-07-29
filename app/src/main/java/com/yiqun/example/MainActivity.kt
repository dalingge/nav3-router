package com.yiqun.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yiqun.example.ui.theme.Nav3routerTheme
import com.yiqun.nav.generated.initNavRegistry
import com.yiqun.nav.runtime.NavCenter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. 初始化 KSP 编译期注册表
        initNavRegistry()

        // 2. 注册业务层全局登录拦截器
        NavCenter.addGlobalInterceptor(AppLoginInterceptor())

        // 3. 压入根首页
        if (NavCenter.primaryStack.backstack.isEmpty()) {
            NavCenter.navigate(HomeScreenDestination())
        }

        setContent {
            Nav3routerTheme {
                NavCenter.Render()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!NavCenter.pop()) {
                finish()
            }

        }
    }
}


