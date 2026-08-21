# Nav3-Router 🚀

[简体中文](README.md) · English

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Navigation 3](https://img.shields.io/badge/Jetpack-Navigation%203%20v1.1.4-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![KSP](https://img.shields.io/badge/KSP-2.3.10-brightgreen.svg)](https://kotlinlang.org/docs/ksp-overview.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Nav3-Router** is a next-generation lightweight, reactive dual-track routing and navigation framework built on the state-driven engine of **Android's official Navigation 3** (`androidx.navigation3:1.1.4`).

It combines **KSP compile-time type safety** with **dynamic URL decoupled routing**, and ships with advanced stack control, a declarative decoupled interceptor chain, **process-death stack restoration**, **one-click DeepLink / push dispatch**, **compile-time route duplication & required-parameter defense**, **404 fault-tolerant fallback**, a **main-thread synchronization lock**, **global/local dual-layer transition animations**, **native Shared Element transitions**, and an onion-layered `NavEntryDecorator` system.

---

## 📐 Architecture & Physical Layering

The project strictly follows modular boundaries with high cohesion and low coupling:

```text
┌──────────────────────────────────────────────────────────┐
│              App business layer (UI & ViewModels)        │
└────────────────────────────┬─────────────────────────────┘
                             │ (fluent DSL init & dual-track navigation)
┌────────────────────────────▼─────────────────────────────┐
│              Framework runtime (:nav-runtime)             │
│  - Minimal chainable config bus (NavCenter)              │
│  - Process death restore (saveState / restoreState)      │
│  - DeepLink / push dispatch (handleIntent & IntentResolver)│
│  - 404 fallback & chain of responsibility (RouteHandler)  │
│  - Runtime interceptor (RouteInterceptor) & decorator      │
└────────────────────────────┬─────────────────────────────┘
                             │ (KSP compile-time scanning)
┌────────────────────────────▼─────────────────────────────┐
│           Pure annotation module (:nav-annotation)        │
│  - @Screen (pure compile-time route marker)              │
│  - @Required (required-parameter validation marker)      │
└────────────────────────────┬─────────────────────────────┘
                             │ (low-level delegation)
┌────────────────────────────▼─────────────────────────────┐
│         Android official engine (androidx.navigation3)    │
│  - NavDisplay (scene rendering / multi-pane / restore)    │
│  - NavEntry (official ViewModelStoreOwner & persistence)  │
└──────────────────────────────────────────────────────────┘
```

---

## 🌟 Core Features

* **Process Death Restoration**: seamlessly serializes the navigation stack into a `Bundle` as a URL list. When the app is killed in the background and restored, it rebuilds the entire page stack in one shot — **zero Parcelable crash risk**.
* **DeepLink / Push decoupled dispatch (`IntentResolver`)**: uniformly handles Scheme external launches, push notifications, widgets, and NFC. Supports custom encrypted push-payload parsing via the `IntentResolver` strategy interface.
* **Compile-time route duplication check**: KSP scans all `@Screen` paths at compile time and aborts the build on duplicate routes, eliminating silent runtime overwrites at the source.
* **404 fault-tolerant fallback (`Fallback Route`)**: supports `setFallbackRoute("app/not_found")` so wrong taps or invalid links degrade gracefully instead of a blank-screen crash.
* **Chain-of-responsibility pre-processing (`RouteHandler`)**: easily extend H5 domain allowlisting, external-browser launching, and custom Scheme interception.
* **Required-parameter defense (`@Required`)**: mark core route parameters with `@Required`; missing parameters throw an explicit runtime exception for fast diagnosis.
* **Native Nav 3 integration**: directly delegates to `navigation3`'s `NavDisplay` and `NavEntry`, inheriting official lifecycle management and ViewModel auto-release.
* **Minimal fluent DSL initialization**: a single `NavCenter` call chain configures animations, interceptors, multi-module routes, and the root screen.
* **Zero-config multi-module architecture**: KSP auto-derives the submodule package name to generate unique extension functions (e.g. `NavCenter.initUser()`) with no naming conflicts and **zero lines of Gradle config**.
* **`NavEntryDecorator` onion system**: natively integrates `rememberViewModelStoreNavEntryDecorator()`; popping a screen triggers `onCleared()` automatically.
* **Dual-layer transitions & shared-element morphing**: supports a global transition, per-screen `@Screen(enterTransition = ...)` overrides, and seamless `.sharedElementKey("key")` cross-page scaling morphs.

---

## 📌 @Screen Route Naming Convention

In `@Screen(route = "...")`, `route` is the globally unique path identifier of a screen. Follow these 5 rules:

| Rule | Description | ✅ Correct | ❌ Wrong |
| :--- | :--- | :--- | :--- |
| **1. Pure path format** | Never include query parameters (they are appended at navigation time) | `@Screen(route = "app/detail")` | `@Screen(route = "app/detail?id={id}")` |
| **2. Two-level module path** | Prefer `[module]/[screen]` to avoid cross-module conflicts | `@Screen(route = "shop/cart")` | `@Screen(route = "cart")` |
| **3. No leading slash** | Omit the leading slash for efficient route-table matching | `@Screen(route = "user/login")` | `@Screen(route = "/user/login")` |
| **4. Lowercase snake_case** | Follow standard URL format to prevent case-mismatch | `@Screen(route = "shop/order_detail")` | `@Screen(route = "Shop/OrderDetail")` |
| **5. Global uniqueness** | Paths must be unique within an app; duplicates trigger a **compile-time error** | Globally unique path | Multiple screens with the same route |

---

## 🚀 Getting Started

### 1. Add dependencies

The framework is published to **Maven Central** — just declare the dependencies in `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

dependencies {
    // Nav3-Router core
    implementation("io.github.dalingge:nav-annotation:1.0.1")  // pure annotation module
    implementation("io.github.dalingge:nav-runtime:1.0.1")     // runtime core module
    ksp("io.github.dalingge:nav-compiler:1.0.1")               // KSP compiler

    // Official Navigation 3 lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:1.1.4")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

// Multi-module projects must set the module name to generate NavCenter.initXxx()
ksp {
    arg("NAV_MODULE_NAME", "user")
}
```

### 2. Flagship initialization (process restore, DeepLink dispatch & 404 fallback)

Configure and decouple restoration in `MainActivity` via the `NavCenter` fluent API:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Fluent DSL initialization of all configuration
        NavCenter
            .init(this)                                                     // bind Context
            .setFallbackRoute("app/not_found")                              // enable 404 fallback route
            .addRouteHandler(WebViewHandler("app/webview", setOf("app.cn")))// allowlisted H5 → local WebView
            .addRouteHandler(BrowserHandler(this))                          // non-allowlisted H5 → system browser
            .addEntryDecorator { rememberViewModelStoreNavEntryDecorator() }// inject official ViewModel scope isolation
            .addEntryDecorator(AnalyticsEntryDecorator())                   // inject custom analytics & onPop cleanup
            .setDefaultTransition(DefaultSlideTransition())                  // configure global transition
            .addGlobalInterceptor(AppLoginInterceptor())                    // register global login interceptor
            .initUser()                                                     // auto-load :feature-user routes
            .initShop()                                                     // auto-load :feature-shop routes
            .initApp()                                                      // auto-load :app routes

        // 2. Decoupled restore trilogy
        val isRestored = NavCenter.restoreState(savedInstanceState) // A. try to restore from process death
        val isIntentHandled = NavCenter.handleIntent(intent)       // B. try to handle DeepLink / push launch

        // C. if neither restored nor externally launched, push the root screen
        if (!isRestored && !isIntentHandled && NavCenter.primaryStack.backstack.isEmpty()) {
            NavCenter.navigate(HomeScreenDestination())
        }

        setContent {
            MaterialTheme {
                NavCenter.Render()
            }
        }
    }

    // 3. Persist the stack before the background process is killed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        NavCenter.saveState(outState)
    }

    // 4. Handle new Scheme / push launches in singleTop / singleTask mode
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NavCenter.handleIntent(intent)
    }

    override fun onBackPressed() {
        if (!NavCenter.pop()) {
            super.onBackPressed()
        }
    }
}
```

---

## 💡 Core Usage Guide

### 1. Process Death Restoration 🆕

When the app is backgrounded and the system kills the process due to memory pressure, the framework serializes and saves the navigation stack. Reopening the app restores all pages automatically:

```kotlin
// 1. Persist the current backstack before the Activity is destroyed
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    NavCenter.saveState(outState)
}

// 2. Restore the entire pre-kill page stack in onCreate in one shot
val isRestored = NavCenter.restoreState(savedInstanceState)
```

---

### 2. DeepLink & Push one-click dispatch (`IntentResolver`) 🆕

The framework automatically takes over Scheme and push launches via `handleIntent`. If your push contains a complex encrypted payload, implement the `IntentResolver` strategy interface:

```kotlin
// Custom encrypted push-payload parsing strategy
class CustomPushIntentResolver : IntentResolver {
    override fun resolve(intent: Intent?): String? {
        if (intent == null) return null
        
        // Prefer the standard Scheme URI (e.g. myapp://shop/detail?id=10086)
        val schemeUrl = intent.dataString
        if (!schemeUrl.isNullOrEmpty()) return schemeUrl

        // Decrypt the target link inside the push Extra (JPush / Getui / etc.)
        val encryptedData = intent.getStringExtra("PUSH_PAYLOAD") ?: return null
        return decryptPushUrl(encryptedData)
    }
}

// Inject via fluent config:
NavCenter.setIntentResolver(CustomPushIntentResolver())

// Trigger a launch:
NavCenter.handleIntent(intent)
```

---

### 3. Declare screens, required parameters `@Required`, and per-screen transitions

```kotlin
@Serializable
data class UserProfile(val id: Int, val name: String)

// Detail screen: mark required params with @Required; missing params throw a safe exception at runtime
@Composable
@Screen(route = "app/detail", needLogin = true)
fun DetailScreen(@Required detailId: Int, user: UserProfile) { ... }

// Dialog screen: override the transition with BottomSheetTransition
@Composable
@Screen(
    route = "app/bottom_dialog",
    enterTransition = BottomSheetTransition::class
)
fun BottomDialogScreen() { ... }
```

---

### 4. Custom `RouteInterceptor`

Implement the `RouteInterceptor` interface (in `:nav-runtime`) for fast synchronous interception and transparent redirection:

```kotlin
class AppLoginInterceptor : RouteInterceptor {
    override fun intercept(url: String): InterceptResult {
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

### 5. Shared Element Transitions

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
                .sharedElementKey(key = avatarKey) // bind Shared Key
        )
        Text("Tap to view the full-size image")
    }
}
```

---

### 6. Pure-Kotlin unit testing (`Navigator`)

```kotlin
class HomeViewModel(private val navigator: Navigator) : ViewModel() {
    fun openDetail(userId: Int) {
        navigator.navigate(DetailScreenDestination(detailId = userId, user = UserProfile(userId, "Aleyn")))
    }
}

// Pure Kotlin unit test (no Android/Robolectric environment required)
@Test
fun testOpenDetail() {
    val fakeNavigator = FakeNavigator()
    val viewModel = HomeViewModel(fakeNavigator)

    viewModel.openDetail(10086)

    assertEquals("app/detail", fakeNavigator.lastDestination?.route)
}
```

---

## 🚀  Advanced Features

### 1. Cross-module UI-less service discovery (`@Service` & `IService`)

For **UI-less business-logic decoupling** in large componentized projects (e.g. `:feature-shop` needs to call the payment service in `:feature-pay` without a direct Gradle dependency between the two), the framework provides a **zero-reflection** service discovery mechanism auto-registered via KSP.

**A. Define the service interface in a common base library:**

```kotlin
// Define the interface in the common base module (:core-common); it must extend IService
interface PayService : IService {
    fun pay(orderId: String, amount: Double): Boolean
}
```

**B. Expose the implementation with `@Service` in the providing module:**

```kotlin
// Implement the interface in the business module (:feature-pay) and mark it with @Service
// contract declares the interface exposed externally; path is an optional string identifier
@Service(contract = PayService::class, path = "pay/service")
class PayServiceImpl : PayService {
    override fun pay(orderId: String, amount: Double): Boolean {
        Log.d("PayService", "Charging order $orderId for $amount CNY")
        return true
    }
}
```

**C. Fetch and call it from any business module (compile-time strong typing via KSP, zero reflection):**

```kotlin
// Option 1: fetch by interface Class (recommended)
val payService = NavCenter.getService<PayService>()
payService?.pay(orderId = "10086", amount = 199.0)

// Option 2: fetch by Path string
val payServiceByPath = NavCenter.getService<PayService>("pay/service")
payServiceByPath?.pay(orderId = "10086", amount = 199.0)
```

### 2. Dynamic path / URL rewriting (`PathReplaceService`)

For **A/B testing**, **online URL error correction**, or **server-pushed route mapping**. Rewrites the original URL at the very front of route dispatch:

**A. Implement the `PathReplaceService` interface:**

```kotlin
// A/B test dynamic path replacer
class ABTestPathReplacer : PathReplaceService {
    override fun replace(rawUrl: String): String {
        // If the raw URL is "pay/detail" and the user is in the A/B test group, rewrite to the 2.0 detail screen
        if (rawUrl == "pay/detail" && ABTestEngine.isGroupA()) {
            return "pay/detail_v2"
        }
        return rawUrl
    }
}
```

**B. Register it in the `NavCenter` fluent chain (multiple rewriting strategies are supported):**

```kotlin
NavCenter
    .addPathReplaceService(ABTestPathReplacer()) // supports registering multiple strategies
    .initUser()
    .navigate(HomeScreenDestination())
```

### 3. Green channel bypass (`greenChannel = true`)

For emergency rescue, elevated-privilege shortcuts, or specific business scenarios, if you need to **force-skip all global and per-route interceptors (`RouteInterceptor`)**, enable the `greenChannel` option when navigating:

```kotlin
// Force-navigate to the detail screen, skipping the global login interceptor and VIP permission interceptor
NavCenter.navigate(DetailScreenDestination(detailId = 100, user = user)) {
    greenChannel = true // enable green channel to bypass interception
}
```

### 4. Global Overlay layer (`showOverlay` & `dismissOverlay`)

In a `NavDisplay`-based navigation system, each `@Screen`-annotated page is recognized as a brand-new route Scene. If you want to show a cashier, a global loading indicator, or a version-update dialog **without switching the current route and keeping the background page fully visible**, use the global Overlay layer mechanism.

**Use cases:**

* **Cross-module service dialogs**: e.g. `:feature-pay` provides a cashier, and `:feature-shop` slides it in above the current checkout page, keeping the checkout page as a fully visible background.
* **Global loading / status dialogs**: an overlay prompt that ignores the current route page.
* **Global update / announcement dialogs**: does not consume route `backstack` depth and lives independently of the navigation stack.

**A. Show a global Overlay layer:**

```kotlin
// Call from a Service implementation, ViewModel, or anywhere — the layer stacks on top of the visible page
NavCenter.showOverlay {
    // Put any pure Compose dialog component here (e.g. ModalBottomSheet / Dialog)
    PayDialog(
        orderId = "ORDER_10086",
        amount = 199.0,
        onPaySuccess = {
            NavCenter.dismissOverlay() // close the layer
            NavCenter.popWithResult("pay_result", true) // pass back the result
        },
        onDismiss = {
            NavCenter.dismissOverlay() // close the layer
        }
    )
}
```

**B. Dismiss the global Overlay layer:**

```kotlin
NavCenter.dismissOverlay()
```

**C. Elegant usage in a `:feature-pay` cross-module service** (combined with `PayService` discovery for zero-UI-coupling dialogs on top of the current page):

```kotlin
@Service(contract = PayService::class, path = "pay/service")
class PayServiceImpl : PayService {

    override fun showPayDialog(orderId: String, amount: Double) {
        // Show the cashier directly above the current active page
        NavCenter.showOverlay {
            PayDialog(
                orderId = orderId,
                amount = amount,
                onPaySuccess = {
                    NavCenter.dismissOverlay()
                    NavCenter.popWithResult("pay_result", true)
                },
                onDismiss = {
                    NavCenter.dismissOverlay()
                    NavCenter.popWithResult("pay_result", false)
                }
            )
        }
    }
}
```

---

## 📖 API Quick Reference

| API | Description |
| :--- | :--- |
| `NavCenter.init(context)` | Bind the global context |
| `NavCenter.saveState(bundle)` | Serialize the current backstack into a Bundle (for process death) 🆕 |
| `NavCenter.restoreState(bundle)` | Restore the pre-kill page stack from a Bundle, returns the restore result 🆕 |
| `NavCenter.handleIntent(intent)` | Parse & dispatch Scheme / DeepLink / push navigation in one step 🆕 |
| `NavCenter.setIntentResolver(resolver)` | Set a custom DeepLink / push parsing strategy 🆕 |
| `NavCenter.setFallbackRoute(route)` | Configure the 404 fallback route |
| `NavCenter.addRouteHandler(handler)` | Register a chain-of-responsibility pre-handler (e.g. WebViewHandler) |
| `NavCenter.navigate(dest, navOptions)` | Strong-typed navigation (SingleTop / PopUpTo / ClearTask), chainable |
| `NavCenter.navigate(url, navOptions)` | URL dynamic navigation (auto URL encode/decode & param matching), chainable |
| `NavCenter.addEntryDecorator(decorator)` | Inject a page decorator (e.g. `rememberViewModelStoreNavEntryDecorator`) |
| `NavCenter.setDefaultTransition(transition)` | Register a global default transition (e.g. `DefaultSlideTransition`) |
| `NavCenter.addGlobalInterceptor(interceptor)` | Register a runtime interceptor (`RouteInterceptor`) |
| `NavCenter.initXxx()` | KSP-generated fluent route-initialization extension functions per module |
| `Modifier.sharedElementKey(key)` | Bind a shared-element key to a component |
| `NavCenter.pop()` | Pop the top screen (main-thread synchronized, return value always reliable) |
| `NavCenter.popWithResult(key, value)` | Pop with a result, returns a boolean synchronously |
| `NavCenter.getResult<T>(key)` | Reactively observe the returned result inside a Composable (nullable-safe) |
| `NavCenter.Render()` | Official Navigation 3 UI rendering entry point |
| `NavCenter.getService<T>()` | Discover a UI-less service instance by interface Class across modules (zero reflection) 🆕 |
| `NavCenter.getService<T>(path)` | Discover a service instance by Path string across modules 🆕 |
| `NavCenter.addPathReplaceService(service)` | Register a dynamic path/URL rewriting strategy (A/B testing, dynamic mapping) 🆕 |
| `NavCenter.navigate(dest) { greenChannel = true }` | Enable green channel to skip all interceptors and force-navigate to the target 🆕 |
| `@Service(contract = KClass, path = "")` | Cross-module service exposure annotation; KSP auto-registers the implementation at compile time 🆕 |
| `NavCenter.showOverlay { content }` | Stack any Compose layer above the current active page (keeping the background visible) 🆕 |
| `NavCenter.dismissOverlay()` | Dismiss the current global Overlay layer 🆕 |
| `NavCenter.currentOverlay` | Composable state object of the current layer (for custom rendering) 🆕 |

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
