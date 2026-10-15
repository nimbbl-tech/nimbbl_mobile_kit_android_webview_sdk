# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Android WebView SDK (`tech.nimbbl:webview-sdk`) — a payment library distributed via Maven Central and JitPack. Merchant apps call `NimbblCheckoutSDK.init()` + `checkout()` and receive results via the `NimbblCheckoutPaymentListener` interface. The checkout UI is a WebView loading a Nimbbl-hosted page.

This SDK depends on `tech.nimbbl:core-api-sdk` (a sibling repo at `../nimbbl_mobile_kit_core_api_sdk`). The Core API SDK must be published to Maven Central before this SDK can be published.

---

## Commands

```bash
# Build debug AAR
./gradlew assembleDebug

# Build release AAR (runs ProGuard)
./gradlew assembleRelease

# Run instrumented tests (requires a connected device/emulator — see TESTING.md for why
# this module has no JVM unit tests: PaymentURLBuilder/NimbblCheckoutOptions need
# android.net.Uri / android.os.Parcel, unavailable without Robolectric)
./gradlew :connectedDebugAndroidTest

# Run a single test class
./gradlew :connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.nimbbl.webviewsdk.utils.PaymentURLBuilderTest

# Lint
./gradlew lint

# Publish to Maven Central (requires .env with credentials + signing keys)
./gradlew publishReleasePublicationToSonatypeRepository --no-daemon

# Version management
./version_manager.sh show
./version_manager.sh bump patch     # 4.0.14 → 4.0.15
./version_manager.sh bump minor
./version_manager.sh set 4.1.0

# Publish to JitPack (snapshot)
./publish_to_jitpack.sh --snapshot

# Publish to JitPack (release tag)
./publish_to_jitpack.sh --release
```

See [TESTING.md](TESTING.md) for the full test-running guide.

### Switching between local and Maven dependency

`build.gradle.kts` line 107 controls how Core API SDK is resolved:

```kotlin
// For local development (default):
implementation(project(":nimbbl_mobile_kit_core_api_sdk"))

// For Maven Central publish (must be active before running publishRelease...):
implementation("tech.nimbbl:core-api-sdk:$coreApiSdkVersion")
```

**Before publishing to Maven Central, switch to the Maven coordinate** — publishing with the `project()` reference produces a broken AAR (POM won't declare the runtime dependency).

---

## Architecture

### Payment flow

```
Merchant Activity
  → NimbblCheckoutSDK.init(activity)       # registers BroadcastReceiver, stores WeakRef
  → NimbblCheckoutSDK.checkout(options)    # launches NimbblCheckoutMainActivity
      → updateOrder() API call (Dispatchers.IO)
      → WebViewManager.setupWebView()      # loads checkout URL
      → JS bridge (INimmbleWebViewInterface) handles UPI intents
      → PaymentHandler.processCallbackURL() on result URL hit
      → PaymentHandler sends local broadcast (BROADCAST_PAYMENT_RESULT)
  → PaymentEventBroadCastReceiver.onReceive()
  → NimbblCheckoutPaymentListener.onCheckoutResponse(MutableMap<String, Any>)
```

### Key classes and their single responsibilities

| Class | Responsibility |
|-------|---------------|
| `NimbblCheckoutSDK` | Singleton. Public API entry point. Owns `BroadcastReceiver` lifecycle and result delivery to merchant listener. |
| `NimbblCheckoutMainActivity` | UI shell. Owns WebView layout, shimmer loading state, UPI intent launch, and back-press forwarding. |
| `WebViewManager` | Configures WebView (security settings, clients, popup windows). Handles callback URL interception and UPI app list injection. |
| `PaymentHandler` | Constructs and sends the `BROADCAST_PAYMENT_RESULT` local broadcast for both success and failure. |
| `PaymentURLBuilder` | Assembles the checkout URL from `NimbblCheckoutOptions` fields and handles browser-back URL merging. `upi_app_code` is appended only when `paymentFlow == "intent"` (never for `"collect"`). |
| `UPIManager` | Queries installed UPI apps (background thread named `"nimbbl-upi-query"`), delivers result on main thread. |
| `ValidationUtils` | Pure input validation; returns typed `ValidationResult.Success` / `ValidationResult.Error`. |

### Singleton state machine

`NimbblCheckoutSDK` is a process-level singleton. Its state across a session:

1. `init()` — creates and registers `PaymentEventBroadCastReceiver` against `applicationContext`
2. `checkout()` — launches Activity; receiver already registered
3. `cleanup()` (called from `onDestroy`) — unregisters receiver, clears all refs

`init()` called a second time unregisters the previous receiver before creating a new one (inside `synchronized(this)`).

### Required merchant integration pattern

```kotlin
class MyActivity : AppCompatActivity(), NimbblCheckoutPaymentListener {

    // Call before checkout()
    private fun startPayment() {
        NimbblCheckoutSDK.getInstance().init(this)
        NimbblCheckoutSDK.getInstance().checkout(options)
    }

    // Required — unregisters the broadcast receiver
    override fun onDestroy() {
        super.onDestroy()
        NimbblCheckoutSDK.getInstance().cleanup()
    }

    override fun onCheckoutResponse(data: MutableMap<String, Any>) {
        // handle result
    }
}
```

**`cleanup()` in `onDestroy()` is mandatory.** Without it, the `BroadcastReceiver` registered in `init()` stays alive for the lifetime of the process. If `init()` is called but the user exits before `checkout()` completes, there is no other code path that unregisters the receiver.

### Context rules

- `NimbblCheckoutSDK` holds `applicationContext` — never Activity context.
- `WebViewManager` receives `applicationContext` — uses `view?.context` (Activity context) only for popup `Dialog` in `onCreateWindow`.
- `PaymentHandler` receives `applicationContext` — `sendBroadcast` works on application context.

### Result delivery guarantee

Results travel as `EXTRA_PAYMENT_DATA` (JSON string) in a local broadcast scoped to the app's package (`setPackage` + `RECEIVER_NOT_EXPORTED`). The broadcast carries only `EXTRA_PAYMENT_DATA` and `EXTRA_ORDER_ID` — the order token (JWT) is intentionally not included. `callbackHandled` (`AtomicBoolean`) in `WebViewManager` prevents double-processing when both `shouldOverrideUrlLoading` and `doUpdateVisitedHistory` fire on the same callback URL.

### WebView security

`applySecuritySettings()` in `WebViewManager` is called for both the main WebView and any popup WebView created in `onCreateWindow`. The same restrictions (`allowFileAccess=false`, `MIXED_CONTENT_NEVER_ALLOW`, `safeBrowsingEnabled`, `setAllowUniversalAccessFromFileURLs=false`) are applied in `preloadWebView()` as well so the WebView is hardened before the checkout URL loads.

---

## Versioning

All version fields live in `version.properties`. `build.gradle.kts` reads them at build time and injects `SDK_VERSION` into `BuildConfig`. `PaymentConstants.SDK_VERSION` reads from `BuildConfig.SDK_VERSION`.

After bumping version:
```bash
git add version.properties
git commit -m "chore(version): bump to $(grep '^SDK_VERSION=' version.properties | cut -d'=' -f2)"
```

---

## Environment / API Endpoints

| Environment | Base URL |
|-------------|----------|
| Production | `https://api.nimbbl.tech` |
| QA | `https://api-qa1.nimbbl.tech` |

Set via `NimbblCheckoutSDK.setEnvironmentUrl()` before `init()`. IP-based URLs are routed to `https://qa3api.qa.nimbbl.tech/` for API calls (useful for local WebView testing).
