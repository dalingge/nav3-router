package com.yiqun.example

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yiqun.example.ui.theme.Nav3routerTheme
import com.yiqun.nav.annotation.Screen
import com.yiqun.nav.runtime.NavCenter
import com.yiqun.nav.runtime.SharedElementTransition
import com.yiqun.nav.runtime.sharedElementKey
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/28  16:49
 */
@Composable
@Screen(route = "app/home")
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val result by NavCenter.getResult<String>("result_key")

    val imageUrl = "image_10086"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("首页 (Home)", style = MaterialTheme.typography.headlineMedium)
        Text("收到 Pop 结果: ${result ?: "无"}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.openDetail(10086) }) {
            Text("强类型跳转 -> 详情页 (传递 UserProfile 对象)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            NavCenter.navigate("app/detail?user=%7B%22id%22%3A999%2C%22name%22%3A%22URLUser%22%7D")
        }) {
            Text("URL 动态跳转 -> 详情页")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            NavCenter.navigate("app/user")
        }) {
            Text("URL 动态跳转 其他模块-> 用户页")
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(onClick = {
            NavCenter.navigate("https://www.app.cn/activity/promo?id=100")
        }) {
            Text("跳转白名单 H5 链接")
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(onClick = {
            NavCenter.navigate("https://www.github.com/kotlin")
        }) {
            Text("跳转非白名单 H5 链接")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            NavCenter.navigate("app/cart")
        }) {
            Text("跳转404 降级页面")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(R.mipmap.image),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp) // 小图
                .clickable {
                    NavCenter.navigate(ImageScreenDestination(imageKey = imageUrl))
                }
                .align(Alignment.CenterHorizontally)
                .sharedElementKey(key = imageUrl)   // 绑定共享元素 Key

        )

        Text("点击查看大图", modifier = Modifier.align(Alignment.CenterHorizontally))

    }
}

@Composable
@Screen(route = "app/detail", needLogin = true)
fun DetailScreen(
    user: UserProfile,
    viewModel: DetailViewModel = viewModel(),
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("详情页 (Detail)", style = MaterialTheme.typography.headlineMedium)
        Text("解析得到 User: ID=${user.id}, Name=${user.name}")
        Text("ViewModel 计数器: ${viewModel.count}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.inc() }) { Text("计数器 +1") }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            NavCenter.popWithResult("result_key", "Result from User ${user.id}")
        }) {
            Text("带结果 Pop 返回")
        }
    }
}

@Composable
@Screen(route = "app/login")
fun LoginScreen(redirect: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("登录页 (Login)", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = {
            UserSession.isLoggedIn = true
            NavCenter.pop()
            if (redirect.isNotEmpty()) {
                NavCenter.navigate(redirect)
            }
        }) {
            Text("模拟登录并重定向恢复")
        }
    }
}


@Composable
@Screen(route = "app/image", enterTransition = SharedElementTransition::class)
fun ImageScreen(imageKey: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.mipmap.image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // 大图
                // 绑定相同的共享元素 Key，框架自动完成缩放平移形变动画！
                .sharedElementKey(key = imageKey)
        )
        Text("详情页内容")
    }
}

/**
 * 本地 H5 渲染容器页面
 */
@Composable
@Screen(route = "app/webview")
fun WebViewScreen(url: String) {
    // 自动对传入的编码 URL 进行解码
    val decodedUrl = remember(url) {
        try {
            URLDecoder.decode(url, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            url
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(decodedUrl)
            }
        },
        update = { webView ->
            webView.loadUrl(decodedUrl)
        }
    )
}


/**
 * 404 降级页面
 */
@Composable
@Screen(route = "app/not_found")
fun NotFoundScreen(url: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("404 ",modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleLarge)

    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Nav3routerTheme {
        HomeScreen()
    }
}