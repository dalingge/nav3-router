# Nav3-Router 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Navigation 3](https://img.shields.io/badge/Jetpack-Navigation%203%20v1.1.4-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![KSP](https://img.shields.io/badge/KSP-2.3.10-brightgreen.svg)](https://kotlinlang.org/docs/ksp-overview.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Nav3-Router** 是一套基于 **Android 官方 Navigation 3 (`androidx.navigation3:1.1.4`)** 状态驱动引擎打造的新一代轻量级、响应式双轨路由与导航框架。

它融合了 **KSP 编译期类型安全** 与 **动态 URL 解耦路由**，内置高级栈控制、声明式解耦拦截链、**全局/局部双层转场动画**、**原生共享元素形变转场（Shared Element）**、原生 ViewModel 作用域隔离以及复杂对象 JSON 序列化支持。

---

## 📐 架构设计

```text
┌──────────────────────────────────────────────────────────┐
│                   App 业务层 (UI & ViewModels)            │
└────────────────────────────┬─────────────────────────────┘
                             │ (注解 @Screen & 类型安全/URL 跳转)
┌────────────────────────────▼─────────────────────────────┐
│                 Nav3-Router 框架层 (:nav-runtime)        │
│  - 双轨制路由调度器 (NavCenter)                            │
│  - 全局/局部双层转场动画 (NavTransition)                   │
│  - 零侵入共享元素上下文透传 (SharedTransitionScope)         │
│  - 声明式解耦拦截链 (RouteInterceptor)                     │
│  - 跨页结果回传总线 (Pop Result API)                      │
└────────────────────────────┬─────────────────────────────┘
                             │ (底层代理)
┌────────────────────────────▼─────────────────────────────┐
│             Android 官方引擎 (androidx.navigation3)         │
│  - NavDisplay (三重转场 / 侧滑预测返回 / 多窗格分屏)        │
│  - NavEntry (官方 ViewModelStoreOwner & 状态恢复)          │
└──────────────────────────────────────────────────────────┘
```

---

## 🌟 核心特性

* **官方 Nav 3 原生对接**：直接代理 `androidx.navigation3:1.1.4` 的 `NavDisplay` 与 `NavEntry`，原生享受官方生命周期管理与 ViewModel 自动释放。
* **双轨制跳转 (Dual-Track Navigation)**：
  * **类型安全轨**：KSP 自动生成 `XxxDestination`，享受 IDE 补全与编译期参数校验。
  * **动态 URL 轨**：支持标准 URL/DeepLink 跨模块跳转（如 `https://domain.com/app/detail?user=...`）。
* **灵活的双层转场动画机制**：
  * **全局默认**：在初始化时配置全局默认动画（如水平滑动/淡入淡出）。
  * **局部覆盖**：在 `@Screen` 注解中按页面维度单独覆写（如底部弹窗滑入）。
  * **官方 Nav 3 三重转场对齐**：原生支持 Push (压栈)、Pop (返回) 与 **Android 14+ 侧滑预测性返回 (Predictive Back)**。
* **零侵入共享元素转场 (Shared Element Transitions)**：基于 `SharedTransitionLayout`，仅需为组件加上 `.sharedElementKey("key")` 即可实现卡片/图片的跨页平滑放大平移形变！
* **复杂对象自动编解码**：支持 `@Serializable` 自定义数据类作为路由参数，KSP 自动生成 URL 编解码与 JSON 序列化逻辑。
* **完全解耦的声明式拦截链**：业务层通过 `NavCenter.addGlobalInterceptor()` 注入拦截策略，支持透明重定向。
* **单元测试友好 (Testable Navigator)**：抽离 `Navigator` 接口，ViewModel 无需依赖 Compose 即可完成纯 Kotlin 单元测试。

---

## 📌 @Screen 路由命名规范

在 `@Screen(route = "...")` 中，`route` 是页面的全局唯一路径标识符，请严格遵循以下 5 大规范：

| 规范 | 规则说明 | ✅ 正确示例 | ❌ 错误示例 |
| :--- | :--- | :--- | :--- |
| **1. 纯 Path 格式** | 绝不包含 Query 参数（参数由跳转动态拼接） | `@Screen(route = "app/detail")` | `@Screen(route = "app/detail?id={id}")` |
| **2. 模块化双层路径** | 推荐采用 `[module]/[screen]` 避免跨模块冲突 | `@Screen(route = "shop/cart")` | `@Screen(route = "cart")` |
| **3. 开头不带 `/`** | 统一省略开头的斜杠，提高路由表匹配效率 | `@Screen(route = "user/login")` | `@Screen(route = "/user/login")` |
| **4. 全小写蛇形命名** | 遵循标准 URL 协议格式，防止大小写误匹配 | `@Screen(route = "shop/order_detail")` | `@Screen(route = "Shop/OrderDetail")` |
| **5. 全局唯一性** | 同一个 App 内路径必须唯一，后注册者会覆盖前者 | 全局唯一 Path | 多个页面配置相同 route |

---

## 🛠️ 快速集成

### 1. 引入 Gradle 依赖

在项目 `build.gradle.kts` 中添加环境依赖版本：

```kotlin
plugins {
    // KSP 插件版本
    id("com.google.devtools.ksp") version "2.3.10"
    // Kotlin Serialization 插件版本
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

dependencies {
    // 官方 Navigation 3 核心库
    implementation("androidx.navigation3:navigation3:1.1.4")
    
    // Router 核心模块
    implementation(project(":nav-annotation"))
    implementation(project(":nav-runtime"))
    ksp(project(":nav-compiler"))
    
    // Kotlinx Serialization JSON 编解码库
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

### 2. 初始化路由、转场与拦截器

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 KSP 自动生成的路由表
        initNavRegistry()

        // 2. 配置全局转场动画 (可配置为 DefaultSlideTransition、FadeTransition 等)
        NavCenter.setDefaultTransition(DefaultSlideTransition())

        // 3. 注入业务层自定义全局拦截器
        NavCenter.addGlobalInterceptor(AppLoginInterceptor())

        // 4. 设置默认根首页
        if (NavCenter.primaryStack.backstack.isEmpty()) {
            NavCenter.navigate(HomeScreenDestination())
        }

        // 5. 绑定 Compose 界面渲染
        setContent {
            MaterialTheme {
                NavCenter.Render()
            }
        }
    }

    override fun onBackPressed() {
        if (!NavCenter.pop()) {
            super.onBackPressed()
        }
    }
}
```

---

## 💡 核心使用指南

### 1. 声明页面与局部转场 `@Screen`

可以使用 `@Screen` 的 `enterTransition` 覆写当前页面的专属转场动画（如底部弹窗）：

```kotlin
@Serializable
data class UserProfile(val id: Int, val name: String)

// 普通页面：未指定 enterTransition，自动降级使用 MainActivity 配置的全局转场动画
@Composable
@Screen(route = "app/detail", needLogin = true)
fun DetailScreen(user: UserProfile) { ... }

// 弹窗页面：局部覆盖为 BottomSheetTransition 底部滑入动画
@Composable
@Screen(
    route = "app/bottom_dialog",
    enterTransition = BottomSheetTransition::class
)
fun BottomDialogScreen() { ... }
```

---

### 2. 共享元素形变转场 (Shared Element Transitions)

利用框架封装的 `Modifier.sharedElementKey()`，给起点与终点组件绑定相同的 Key 即可实现图片/卡片的无缝跨页放大型变：

#### 列表页（起点小图）：
```kotlin
@Composable
@Screen(route = "app/home")
fun HomeScreen() {
    val avatarKey = "user_avatar_10086"

    Row(modifier = Modifier.clickable {
        NavCenter.navigate(DetailScreenDestination(avatarKey = avatarKey))
    }) {
        Image(
            painter = painterResource(R.drawable.avatar),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp) // 起点：小图
                .sharedElementKey(key = avatarKey) // 绑定 Shared Key
        )
        Text("点击查看大图")
    }
}
```

#### 详情页（终点大图）：
```kotlin
@Composable
@Screen(
    route = "app/detail",
    enterTransition = SharedElementTransition::class // 容器淡入，完全交由共享元素做形变
)
fun DetailScreen(avatarKey: String) {
    Column {
        Image(
            painter = painterResource(R.drawable.avatar),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // 终点：大图
                .sharedElementKey(key = avatarKey) // 绑定相同的 Shared Key
        )
        Text("详情页内容")
    }
}
```

---

### 3. 双轨导航跳转与高级栈控制

```kotlin
// 轨迹 A：强类型跳转（支持复杂对象 & NavOptions 高级栈控制）
NavCenter.navigate(
    DetailScreenDestination(user = UserProfile(id = 10086, name = "Aleyn"))
) {
    launchSingleTop = true
    popUpToRoute = "app/home"
}

// 轨迹 B：URL 动态解耦跳转 (适用于 DeepLink / H5 唤起 / 跨模块)
val userJson = Json.encodeToString(UserProfile(10086, "Aleyn"))
NavCenter.navigate("https://www.app.cn/app/detail?user=$userJson")
```

---

### 4. App 业务层解耦登录拦截器

```kotlin
class AppLoginInterceptor : RouteInterceptor {
    override suspend fun intercept(url: String): InterceptResult {
        val uri = Uri.parse(url)
        val path = uri.path?.removePrefix("/") ?: uri.schemeSpecificPart
        val meta = NavRegistry.getMeta(path)

        if (meta?.needLogin == true && !UserSession.isLoggedIn) {
            val encodedTarget = URLEncoder.encode(url, "UTF-8")
            return InterceptResult.Redirect("app/login?redirect=$encodedTarget")
        }

        return InterceptResult.Proceed
    }
}

@Composable
@Screen(route = "app/login")
fun LoginScreen(redirect: String) {
    Button(onClick = {
        UserSession.isLoggedIn = true
        NavCenter.pop()
        if (redirect.isNotEmpty()) {
            NavCenter.navigate(URLDecoder.decode(redirect, "UTF-8"))
        }
    }) {
        Text("模拟登录成功并原路恢复跳转")
    }
}
```

---

### 5. ViewModel 解耦与纯 Kotlin 单元测试

```kotlin
class HomeViewModel(private val navigator: Navigator) : ViewModel() {
    fun openDetail(userId: Int) {
        navigator.navigate(DetailScreenDestination(user = UserProfile(userId, "Aleyn")))
    }
}

// 纯 Kotlin 单元测试 (无需 Android/Robolectric 环境)
@Test
fun testOpenDetail() {
    val fakeNavigator = FakeNavigator()
    val viewModel = HomeViewModel(fakeNavigator)

    viewModel.openDetail(10086)

    assertEquals("app/detail", fakeNavigator.lastDestination?.route)
}
```

---

### 6. 跨页面结果回传 (Result State API)

```kotlin
// 目标页：出栈并传递结果
NavCenter.popWithResult("result_key", "Result Data")

// 发起页：响应式监听消费
@Composable
@Screen(route = "app/home")
fun HomeScreen() {
    val result by NavCenter.getResult<String>("result_key")
    Text("收到结果: ${result ?: "无"}")
}
```

---

## 📖 API 速查表

| API | 功能描述 |
| :--- | :--- |
| `NavCenter.navigate(dest, navOptions)` | 强类型跳转（支持 SingleTop / PopUpTo / ClearTask） |
| `NavCenter.navigate(url, navOptions)` | URL 动态跳转（自动 URL 编解码 & 参数匹配） |
| `NavCenter.pop()` | 栈顶页面出栈 |
| `NavCenter.popWithResult(key, value)` | 携带结果出栈 |
| `NavCenter.getResult<T>(key)` | Composable 内部响应式监听回传结果 |
| `NavCenter.setDefaultTransition(transition)` | 注册全局默认转场动画（如 `DefaultSlideTransition`） |
| `Modifier.sharedElementKey(key)` | 为组件绑定共享元素 Key |
| `NavCenter.addGlobalInterceptor(interceptor)` | 动态注册业务层全局路由拦截器 |
| `NavCenter.Render()` | 官方 Navigation 3 UI 渲染总入口 |
| `Navigator` | 抽象导航接口，用于 ViewModel 依赖注入与单元测试 |

---

## 📄 License

```text
Copyright 2024 Nav3-Router Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
```