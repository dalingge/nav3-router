# Nav3-Router 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Navigation 3](https://img.shields.io/badge/Jetpack-Navigation%203%20v1.1.4-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![KSP](https://img.shields.io/badge/KSP-2.3.10-brightgreen.svg)](https://kotlinlang.org/docs/ksp-overview.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Nav3-Router** 是一套基于 **Android 官方 Navigation 3 (`androidx.navigation3:1.1.4`)** 状态驱动引擎打造的新一代轻量级、响应式双轨路由与导航框架。

它融合了 **KSP 编译期类型安全** 与 **动态 URL 解耦路由**，内置高级栈控制、声明式解耦拦截链、**全局/局部双层转场动画**、**原生共享元素形变转场（Shared Element）**、**NavEntryDecorator 装饰器体系**（自动状态恢复 + 原生 ViewModel 作用域隔离）以及复杂对象 JSON 序列化支持。

---

## 📐 架构设计

```text
┌──────────────────────────────────────────────────────────┐
│                   App 业务层 (UI & ViewModels)            │
└────────────────────────────┬─────────────────────────────┘
                             │ (流式 DSL 链式初始化 & 双轨跳转)
┌────────────────────────────▼─────────────────────────────┐
│                 Nav3-Router 框架层 (:nav-runtime)        │
│  - 极简链式配置总线 (NavCenter)                            │
│  - 多模块 KSP 包名自动解析扩展 (initUser / initShop)        │
│  - 装饰器洋葱皮体系 (NavEntryDecorator & onPop 清理)       │
│  - 三重转场 (Push / Pop / Predictive Back 侧滑预测)        │
│  - 零侵入共享元素上下文透传 (SharedTransitionScope)         │
│  - 声明式解耦拦截链 (RouteInterceptor)                     │
└────────────────────────────┬─────────────────────────────┘
                             │ (底层代理)
┌────────────────────────────▼─────────────────────────────┐
│             Android 官方引擎 (androidx.navigation3)         │
│  - NavDisplay (场景渲染 / 多窗格分屏 / 状态自动恢复)         │
│  - NavEntry (官方 ViewModelStoreOwner & 状态持久化)        │
└──────────────────────────────────────────────────────────┘
```

---

## 🌟 核心特性

* **官方 Nav 3 原生对接**：直接代理 `androidx.navigation3:1.1.4` 的 `NavDisplay` 与 `NavEntry`，原生享受官方生命周期管理与 ViewModel 自动释放。
* **极致流式 DSL 初始化**：通过 `NavCenter` 链式调用一次性搞定动画配置、拦截器注册、多模块路由加载与首页压栈。
* **零配置多模块架构**：KSP 自动分析子模块包名生成唯一扩展函数（如 `NavCenter.initUser()`），无同名冲突，**0 行 Gradle 配置**！
* **`NavEntryDecorator` 装饰器洋葱皮体系**：
  * **底层自动置顶**：强制保留官方 `rememberSaveableStateHolderNavEntryDecorator`，确保 TextField 输入框与列表滚动位置永远不丢失。
  * **ViewModel 隔离**：原生集成 `lifecycle-viewmodel-navigation3` 的 `rememberViewModelStoreNavEntryDecorator()`，页面出栈自动触发 `onCleared()`。
  * **自定义扩展**：支持实现 `NavEntryDecorator(onPop = { ... }, decorate = { ... })` 实现自动化页面曝光埋点与内存清理。
* **双轨制跳转 (Dual-Track Navigation)**：
  * **类型安全轨**：KSP 自动生成 `XxxDestination`，享受 IDE 补全与编译期参数校验。
  * **动态 URL 轨**：支持标准 URL/DeepLink 跨模块跳转（如 `https://domain.com/app/detail?user=...`）。
* **灵活的双层转场动画机制**：
  * **全局默认**：在初始化时配置全局默认动画（如水平滑动/淡入淡出）。
  * **局部覆盖**：在 `@Screen` 注解中按页面维度单独覆写（如底部弹窗滑入）。
  * **官方 Nav 3 三重转场对齐**：原生支持 Push (压栈)、Pop (返回) 与 **Android 14+ 侧滑预测性返回 (Predictive Back)**。
* **零侵入共享元素转场 (Shared Element Transitions)**：基于 `SharedTransitionLayout`，仅需给组件加上 `.sharedElementKey("key")` 即可实现卡片/图片的跨页平滑放大平移形变！
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

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

dependencies {
    // 官方 Navigation 3 核心库
    implementation("androidx.navigation3:navigation3:1.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:1.1.4")
    
    // Router 核心模块
    implementation(project(":nav-annotation"))
    implementation(project(":nav-runtime"))
    ksp(project(":nav-compiler"))
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

### 2. 极简流式 DSL 初始化

在 `MainActivity` 中通过 `NavCenter` 链式 API 完成所有配置：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 像 DSL 一样极简、流畅地链式初始化整个 App！
        NavCenter
            .addEntryDecorator { rememberViewModelStoreNavEntryDecorator() } // 1. 注入官方 ViewModel 作用域隔离
            .addEntryDecorator(AnalyticsEntryDecorator())                   // 2. 注入自定义全埋点与 onPop 清理
            .setDefaultTransition(DefaultSlideTransition())                  // 3. 配置全局转场动画
            .addGlobalInterceptor(AppLoginInterceptor())                    // 4. 注册全局登录拦截器
            .initUser()                                                     // 5. 自动加载 :feature-user 模块路由
            .initShop()                                                     // 6. 自动加载 :feature-shop 模块路由
            .initApp()                                                      // 7. 自动加载 :app 模块路由
            .navigate(HomeScreenDestination())                               // 8. 压入根首页

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

### 2. 自定义 `NavEntryDecorator` 与 `onPop` 清理

继承官方 `NavEntryDecorator`，利用 `onPop` 回调与 `entry.Content()` 实现页面级的全埋点曝光与内存释放：

```kotlin
class AnalyticsEntryDecorator : NavEntryDecorator<NavDestination>(
    // 1. 当页面 Pop 出栈且彻底离开 Composition 时触发，进行资源清理
    onPop = { contentKey ->
        println("📊 [页面 Pop 出栈] 彻底清理 contentKey = $contentKey 的缓存数据")
    },
    // 2. 装饰闭包：渲染页面 UI
    decorate = { entry ->
        val destination = entry.metadata["destination"] as? NavDestination

        LaunchedEffect(destination) {
            if (destination != null) {
                println("📊 [页面曝光] route = ${destination.route}")
            }
        }

        // 核心：调用官方 entry.Content() 渲染页面！
        entry.Content()
    }
)
```

---

### 3. 共享元素形变转场 (Shared Element Transitions)

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

### 4. 双轨导航跳转与高级栈控制

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

### 5. App 业务层解耦登录拦截器

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

### 6. ViewModel 解耦与纯 Kotlin 单元测试

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

### 7. 跨页面结果回传 (Result State API)

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
| `NavCenter.navigate(dest, navOptions)` | 强类型跳转（支持 SingleTop / PopUpTo / ClearTask），支持链式调用 |
| `NavCenter.navigate(url, navOptions)` | URL 动态跳转（自动 URL 编解码 & 参数匹配），支持链式调用 |
| `NavCenter.addEntryDecorator(decorator)` | 动态注入页面 Decorator（如 `rememberViewModelStoreNavEntryDecorator`） |
| `NavCenter.setDefaultTransition(transition)` | 注册全局默认转场动画（如 `DefaultSlideTransition`） |
| `NavCenter.addGlobalInterceptor(interceptor)` | 动态注册业务层全局路由拦截器 |
| `NavCenter.initXxx()` | 多模块 KSP 自动生成的流式路由初始化扩展函数 |
| `Modifier.sharedElementKey(key)` | 为组件绑定共享元素 Key |
| `NavCenter.pop()` | 栈顶页面出栈 |
| `NavCenter.popWithResult(key, value)` | 携带结果出栈 |
| `NavCenter.getResult<T>(key)` | Composable 内部响应式监听回传结果 |
| `NavCenter.Render()` | 官方 Navigation 3 UI 渲染总入口 |

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