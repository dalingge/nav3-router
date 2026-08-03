# Nav3-Router 🚀

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Navigation 3](https://img.shields.io/badge/Jetpack-Navigation%203%20v1.1.4-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![KSP](https://img.shields.io/badge/KSP-2.3.10-brightgreen.svg)](https://kotlinlang.org/docs/ksp-overview.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Nav3-Router** 是一套基于 **Android 官方 Navigation 3** 状态驱动引擎打造的新一代轻量级、响应式双轨路由与导航框架。

它融合了 **KSP 编译期类型安全** 与 **动态 URL 解耦路由**，内置高级栈控制、声明式解耦拦截链、**编译期查重与必传参数防御**、**404 容错降级**、**抗并发协程 Mutex 锁**、**全局/局部双层转场动画**、**原生共享元素形变转场（Shared Element）** 以及 `NavEntryDecorator` 装饰器洋葱皮体系。

---

## 📐 架构设计与物理分层

项目严格遵循高内聚、低耦合的模块化边界划分：

```text
┌──────────────────────────────────────────────────────────┐
│                   App 业务层 (UI & ViewModels)            │
└────────────────────────────┬─────────────────────────────┘
                             │ (流式 DSL 链式初始化 & 双轨跳转)
┌────────────────────────────▼─────────────────────────────┐
│                 框架运行时 (:nav-runtime)                  │
│  - 极简链式配置总线 (NavCenter)                            │
│  - 协程并发互斥锁 (Mutex 防御多点崩溃)                     │
│  - 404 容错降级与多 Tab 独立子栈 (TabNavHost)              │
│  - 运行时拦截链 (RouteInterceptor) 与装饰器 (NavEntry)     │
└────────────────────────────┬─────────────────────────────┘
                             │ (KSP 编译期扫描)
┌────────────────────────────▼─────────────────────────────┐
│               纯净注解模块 (:nav-annotation)               │
│  - @Screen (纯编译期路由标记)                              │
│  - @Required (必传参数校验标记)                            │
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

* **编译期路由查重 (`Route Duplication Check`)**：KSP 编译时自动扫描所有 `@Screen` 路径，若发现重复 route 直接中断构建，从源头上消除了线上静默覆盖隐患。
* **404 容错降级机制 (`Fallback Route`)**：支持配置 `setFallbackRoute("app/not_found")`，当用户误点或下发错误链接时自动平滑降级，绝不白屏崩溃。
* **抗并发协程互斥锁 (`Mutex Protection`)**：内置协程 Mutex 锁，完美抵御短时间内多次快速点击导致的导航状态错乱与闪退。
* **必传参数安全防御 (`@Required`)**：对核心路由参数使用 `@Required` 标记，若跳转时漏传参数会在运行时抛出显式异常，便于快速定位问题。
* **运行时拦截契约下沉 (`RouteInterceptor`)**：`RouteInterceptor` 位于 `:nav-runtime`，完美支持异步协程挂起校验（如登录、VIP 鉴权、强制升级检测）。
* **官方 Nav 3 原生对接**：直接代理 `navigation3` 的 `NavDisplay` 与 `NavEntry`，原生享受官方生命周期管理与 ViewModel 自动释放。
* **极简流式 DSL 初始化**：通过 `NavCenter` 链式调用一次性搞定动画配置、拦截器注册、多模块路由加载与首页压栈。
* **零配置多模块架构**：KSP 自动分析子模块包名生成唯一扩展函数（如 `NavCenter.initUser()`），无同名冲突，**0 行 Gradle 配置**。
* **`NavEntryDecorator` 装饰器洋葱皮体系**：原生集成 `rememberViewModelStoreNavEntryDecorator()`，页面出栈自动触发 `onCleared()`。
* **双层转场动画与共享元素形变**：支持全局转场、局部 `@Screen(enterTransition = ...)` 覆写，以及 `.sharedElementKey("key")` 无缝跨页放大型变。

---

## 📌 @Screen 路由命名规范

在 `@Screen(route = "...")` 中，`route` 是页面的全局唯一路径标识符，请严格遵循以下 5 大规范：

| 规范 | 规则说明 | ✅ 正确示例 | ❌ 错误示例 |
| :--- | :--- | :--- | :--- |
| **1. 纯 Path 格式** | 绝不包含 Query 参数（参数由跳转动态拼接） | `@Screen(route = "app/detail")` | `@Screen(route = "app/detail?id={id}")` |
| **2. 模块化双层路径** | 推荐采用 `[module]/[screen]` 避免跨模块冲突 | `@Screen(route = "shop/cart")` | `@Screen(route = "cart")` |
| **3. 开头不带 `/`** | 统一省略开头的斜杠，提高路由表匹配效率 | `@Screen(route = "user/login")` | `@Screen(route = "/user/login")` |
| **4. 全小写蛇形命名** | 遵循标准 URL 协议格式，防止大小写误匹配 | `@Screen(route = "shop/order_detail")` | `@Screen(route = "Shop/OrderDetail")` |
| **5. 全局唯一性** | 同一个 App 内路径必须唯一，重复会触发**编译期报错** | 全局唯一 Path | 多个页面配置相同 route |

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
    
    // Router 模块划分
    implementation(project(":nav-annotation")) // 纯注解模块
    implementation(project(":nav-runtime"))    // 运行时核心模块
    ksp(project(":nav-compiler"))              // KSP 编译器
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

### 2. 企业级旗舰初始化 (含 404 降级与互斥锁保护)

在 `MainActivity` 中通过 `NavCenter` 链式 API 完成所有配置：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 像 DSL 一样极简、流畅地链式初始化整个 App！
        NavCenter
            .init(this)                                                     // 1. 绑定 Context
            .setFallbackRoute("app/not_found")                              // 2. 开启 404 容错降级路由
            .addEntryDecorator { rememberViewModelStoreNavEntryDecorator() }// 3. 注入官方 ViewModel 作用域隔离
            .addEntryDecorator(AnalyticsEntryDecorator())                   // 4. 注入自定义全埋点与 onPop 清理
            .setDefaultTransition(DefaultSlideTransition())                  // 5. 配置全局转场动画
            .addGlobalInterceptor(AppLoginInterceptor())                    // 6. 注册全局登录拦截器
            .initUser()                                                     // 7. 自动加载 :feature-user 模块路由
            .initShop()                                                     // 8. 自动加载 :feature-shop 模块路由
            .initApp()                                                      // 9. 自动加载 :app 模块路由
            .navigate(HomeScreenDestination())                              // 10. 压入根首页

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

### 1. 声明页面、必传参数 `@Required` 与局部转场

```kotlin
@Serializable
data class UserProfile(val id: Int, val name: String)

// 详情页：使用 @Required 标记必传参数，未传时运行时抛出安全异常
@Composable
@Screen(route = "app/detail", needLogin = true)
fun DetailScreen(@Required detailId: Int, user: UserProfile) { ... }

// 弹窗页面：局部覆盖为 BottomSheetTransition 底部滑入动画
@Composable
@Screen(
    route = "app/bottom_dialog",
    enterTransition = BottomSheetTransition::class
)
fun BottomDialogScreen() { ... }
```

---

### 2. 自定义 `RouteInterceptor` 拦截器（位于 `:nav-runtime`）

实现挂起函数 `intercept`，支持异步网络校验与透明重定向：

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
```

---

### 3. 共享元素形变转场 (Shared Element Transitions)

```kotlin
@Composable
@Screen(route = "app/home")
fun HomeScreen() {
    val avatarKey = "user_avatar_10086"

    Row(modifier = Modifier.clickable {
        NavCenter.navigate(DetailScreenDestination(detailId = 1, user = UserProfile(1, "A")))
    }) {
        Image(
            painter = painterResource(R.drawable.avatar),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .sharedElementKey(key = avatarKey) // 绑定 Shared Key
        )
        Text("点击查看大图")
    }
}
```

---

### 4. 纯 Kotlin 单元测试 (`Navigator`)

```kotlin
class HomeViewModel(private val navigator: Navigator) : ViewModel() {
    fun openDetail(userId: Int) {
        navigator.navigate(DetailScreenDestination(detailId = userId, user = UserProfile(userId, "Aleyn")))
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

## 📖 API 速查表

| API | 功能描述 |
| :--- | :--- |
| `NavCenter.setFallbackRoute(route)` | 配置 404 路由降级兜底路径 |
| `NavCenter.navigate(dest, navOptions)` | 强类型跳转（支持 SingleTop / PopUpTo / ClearTask），支持链式调用 |
| `NavCenter.navigate(url, navOptions)` | URL 动态跳转（自动 URL 编解码 & 参数匹配），支持链式调用 |
| `NavCenter.addEntryDecorator(decorator)` | 动态注入页面 Decorator（如 `rememberViewModelStoreNavEntryDecorator`） |
| `NavCenter.setDefaultTransition(transition)` | 注册全局默认转场动画（如 `DefaultSlideTransition`） |
| `NavCenter.addGlobalInterceptor(interceptor)` | 动态注册运行时挂起拦截器（`RouteInterceptor`） |
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