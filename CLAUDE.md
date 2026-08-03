# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在此仓库中工作时提供指引。

## 构建命令

```bash
# 构建整个项目
./gradlew assembleDebug

# 构建并安装到已连接的设备/模拟器
./gradlew installDebug

# 运行所有单元测试
./gradlew test

# 运行指定模块的单元测试
./gradlew :nav-runtime:test
./gradlew :nav-compiler:test

# 清理构建
./gradlew clean assembleDebug
```

项目未配置 lint 或代码格式化工具，仅通过 `gradle.properties` 中的 `kotlin.code.style=official` 声明了 Kotlin 官方代码风格。

## 项目架构

**nav3-router** 是一套基于 **Android 官方 Navigation 3**（`androidx.navigation3` v1.1.4）构建的轻量级 Android 导航框架。提供 KSP 编译期路由代码生成、双轨导航（强类型 + URL）、拦截器链、共享元素转场动画以及多模块支持。

### 模块结构

| 模块 | 类型 | 用途 |
|---|---|---|
| `:nav-annotation` | 纯 Kotlin/JVM | 定义 `@Screen`、`RouteInterceptor`、`InterceptResult`、`UnspecifiedTransition` |
| `:nav-compiler` | 纯 Kotlin/JVM + KSP | KSP 处理器，为每个 `@Screen` 页面生成 `XxxDestination` 数据类与 `NavCenter.initXxx()` 扩展函数 |
| `:nav-runtime` | Android 库 | 核心运行时：`NavCenter`、`NavTransition`、`NavRegistry`、`NavStack`、装饰器体系、路由处理器 |
| `:app` | Android 应用 | 示例消费者应用，演示框架全部功能 |
| `:user` | Android 库 | 示例功能模块，展示多模块路由注册方式 |

### 依赖流向

```
nav-annotation（合约层）
    ↑ 依赖
nav-compiler（KSP 处理器，使用 KotlinPoet）
nav-runtime（运行时引擎）
    ↑ api 依赖 nav-annotation
app ──► api 依赖 nav-runtime，ksp 依赖 nav-compiler
user ──► api 依赖 nav-runtime，ksp 依赖 nav-compiler
```

### 核心架构

**`NavCenter`**（`nav-runtime/.../NavCenter.kt`）是全局导航中心单例，实现了 `Navigator` 接口。管理全局回退栈（`NavStack`，底层为 `SnapshotStateList`）、路由表、拦截器、装饰器和转场动画。所有可链式调用的配置方法（`.addEntryDecorator()`、`.setDefaultTransition()`、`.addGlobalInterceptor()`、`.initXxx()`、`.navigate()`）均返回 `NavCenter` 自身，支持在 `MainActivity.onCreate()` 中以流式 DSL 风格完成一次性初始化。

**`NavRegistry`** 以路由字符串（如 `"app/detail"`）为键存储 `RouteMeta` 对象。`RouteMeta` 包含路由路径、`needLogin` 标记、`factory` 工厂 lambda（URL 查询参数 → `NavDestination`）、`content` 页面组合函数、路由级拦截器列表以及可选的路由级 `NavTransition`。

**导航管道**（`NavCenter.navigate(url)` 内部执行流程）：
1. `RouteHandler` 处理器链 — 第一层过滤（例如：`WebViewHandler` 拦截白名单域名 HTTP URL，转发到应用内 WebView 页面；`BrowserHandler` 拦截其余 HTTP URL，跳转系统浏览器）
2. URL 解析 → 通过 `NavRegistry.getMeta(path)` 匹配路由
3. 全局 `RouteInterceptor` 拦截器链（返回 `Proceed` / `Redirect` / `Abort`）
4. 路由级 `RouteInterceptor` 拦截器链（来自 `@Screen(interceptors = [...])`）
5. 栈控制策略（`clearTask`、`popUpTo`、`launchSingleTop`）
6. 通过 `meta.factory(queryParams)` 构建 `NavDestination` → 追加到 `primaryStack.backstack`

**转场动画**（`NavTransition` 接口）：提供 `pushEnter()`/`pushExit()`/`popEnter()`/`popExit()` 四个方法。内置实现：`DefaultSlideTransition`（水平滑动）、`BottomSheetTransition`（底部弹窗）、`SharedElementTransition`（淡入淡出，配合共享元素使用）。全局默认转场通过 `NavCenter.setDefaultTransition()` 设置，页面级覆盖通过 `@Screen(enterTransition = XxxTransition::class)` 声明。

**Entry 装饰器**（`NavEntryDecorator`）：包裹在每个 `NavEntry` 外层的可组合函数。官方 `rememberSaveableStateHolderNavEntryDecorator` 始终置于最内层（保证 TextField 输入与列表滚动位置不丢失）。额外装饰器（如 `rememberViewModelStoreNavEntryDecorator`、自定义埋点装饰器）按注册顺序依次包裹。

**KSP 代码生成**（`NavSymbolProcessor`）：
- 扫描带有 `@Screen` 注解的 Composable 函数
- 生成实现 `NavDestination` 接口的 `XxxDestination` 数据类（含 `route`、`toUrl()`）
- 生成 `NavCenter.initXxx()` 扩展函数（位于 `com.yiqun.nav.generated` 包），为每个发现的页面调用 `NavRegistry.register(RouteMeta(...))`
- 模块名称：优先读取 `ksp { arg("NAV_MODULE_NAME", "user") }` 配置，未配置时从源码包名最后一段推导
- 基于 KotlinPoet + KSP，通过 `@AutoService(SymbolProcessorProvider)` 注册

### 关键模式

- **多模块路由注册**：每个使用 KSP 的模块会生成一个 `NavCenter.initXxx()` 扩展函数。`:app` 模块在 `MainActivity` 中链式调用：`.initYiqun().initUser()`。`:user` 模块在其 `build.gradle.kts` 中配置 `ksp { arg("NAV_MODULE_NAME", "user") }`。

- **双轨导航**：强类型跳转（`NavCenter.navigate(DetailScreenDestination(user = ...))`）与 URL 字符串跳转（`NavCenter.navigate("app/detail?user=...")`）。两者最终都走 `navigate(url)` 内部逻辑——强类型版本先调用 `destination.toUrl()` 转换为 URL。

- **路由命名规范**：`@Screen(route = "app/detail")`——全小写，两段式（`模块/页面`），无前导斜杠，路由字符串中不包含查询参数。

- **跨页面结果回传**：`NavCenter.popWithResult(key, value)` + `NavCenter.getResult<T>(key)`，返回 `State<T?>` 供 Composable 响应式消费。

- **共享元素转场**：`Modifier.sharedElementKey(key)` 封装了 `SharedTransitionScope.sharedElement()`，通过 `NavHostContainer` 内的 `SharedTransitionLayout` 所提供的 `CompositionLocal` 向下透传。

- **Tab 导航**：`rememberTabNavigatorState(vararg tabs)` + `TabNavHost(state)`——每个 Tab 拥有独立的 `NavStack`，仅当前选中的 Tab 参与 Composition。
