package com.yiqun.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.yiqun.example.ui.theme.Nav3routerTheme
import com.yiqun.nav.generated.initUser
import com.yiqun.nav.generated.initYiqun
import com.yiqun.nav.runtime.DefaultSlideTransition
import com.yiqun.nav.runtime.NavCenter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NavCenter
            .setDefaultTransition(DefaultSlideTransition()) // 注册全局动画
            .addGlobalInterceptor(AppLoginInterceptor())   // 注册全局拦截器
            .addEntryDecorator { rememberViewModelStoreNavEntryDecorator() }
            .addEntryDecorator(AnalyticsEntryDecorator())//传入自定义的曝光埋点与 onPop 清理装饰器
            .initYiqun()
            .initUser() //注册 User 模块路由
            .navigate(HomeScreenDestination())  //  压入根首页

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


