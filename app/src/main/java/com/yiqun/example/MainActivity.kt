package com.yiqun.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.yiqun.example.ui.theme.Nav3routerTheme
import com.yiqun.nav.generated.initUser
import com.yiqun.nav.generated.initYiqun
import com.yiqun.nav.runtime.DefaultSlideTransition
import com.yiqun.nav.runtime.NavCenter
import com.yiqun.nav.runtime.handler.BrowserHandler
import com.yiqun.nav.runtime.handler.WebViewHandler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NavCenter
            .setFallbackRoute("app/not_found")
            .addRouteHandler(WebViewHandler("app/webview", setOf("app.cn", "domain.com", "baidu.com")))
            .addRouteHandler(BrowserHandler(this))
            .setDefaultTransition(DefaultSlideTransition()) // 注册全局动画
            .addGlobalInterceptor(AppLoginInterceptor())   // 注册全局拦截器
            .addEntryDecorator { rememberViewModelStoreNavEntryDecorator() }
            .addEntryDecorator(AnalyticsEntryDecorator())//传入自定义的曝光埋点与 onPop 清理装饰器
            .initYiqun()
            .initUser() //注册 User 模块路由

        // 解耦恢复逻辑三部曲：
        // 优先尝试从进程被杀恢复 (savedInstanceState)
        val isRestored = NavCenter.restoreState(savedInstanceState)

        // 尝试从外部 Intent / DeepLink / 推送唤起
        val isIntentHandled = NavCenter.handleIntent(intent)

        // 若既没有进程恢复，也没有外部 DeepLink，则默认压入根首页
        if (!isRestored && !isIntentHandled && NavCenter.primaryStack.backstack.isEmpty()) {
            NavCenter.navigate(HomeScreenDestination())  //  压入根首页
        }

        setContent {
            Nav3routerTheme {
                NavCenter.Render()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!NavCenter.pop()) {
                finish() }

        }
    }

    // 响应 Activity 在后台被杀死前的状态保存
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        NavCenter.saveState(outState) // 将当前导航栈持久化保存
    }

    //  响应 Activity 为 singleTop/singleTask 模式下的外部 Scheme / 推送新唤起
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NavCenter.handleIntent(intent)
    }
}


