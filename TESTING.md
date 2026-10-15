# Running Tests

Most of this module's logic (`PaymentURLBuilder`, `NimbblCheckoutOptions`, `NimbblCheckoutSDK`,
`NimbblCheckoutMainActivity`, most of `utils`) needs `android.net.Uri`/`android.os.Parcel`/a real
Activity lifecycle, unavailable in plain JVM tests here (no Robolectric), so most tests live under
`src/androidTest` and run on a real device/emulator. A few pure-logic pieces (`SDKUtils`'s
`createErrorResponse`/`getStackTrace`) do run as plain JVM tests.

## Unit tests (JVM, no device needed)

```bash
./gradlew :testDebugUnitTest
```

[`SDKUtilsTest`](src/test/java/tech/nimbbl/webviewsdk/utils/SDKUtilsTest.kt) — the handful of
`SDKUtils` functions with no Android API calls on their happy path.

## Instrumented tests (requires a connected device or running emulator)

```bash
# All instrumented tests in this module (the leading `:` scopes it to the root
# project — without it, Gradle also runs the included core_api_sdk submodule's tests)
./gradlew :connectedDebugAndroidTest

# A single test class, e.g.:
./gradlew :connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=tech.nimbbl.webviewsdk.utils.PaymentURLBuilderTest
```

Check a device/emulator is attached first with `adb devices`.

Key suites:
- [`PaymentURLBuilderTest`](src/androidTest/java/tech/nimbbl/webviewsdk/utils/PaymentURLBuilderTest.kt)
  — query-param building, incl. `upi_app_code` only for the `intent` payment flow.
- [`NimbblCheckoutOptionsTest`](src/androidTest/java/tech/nimbbl/webviewsdk/models/NimbblCheckoutOptionsTest.kt)
  — `Builder.build()` and the `Parcelable` round-trip for every field.
- [`NimbblCheckoutSDKTest`](src/androidTest/java/tech/nimbbl/webviewsdk/core/NimbblCheckoutSDKTest.kt)
  — singleton lifecycle, validation-error paths, and (the one `CLAUDE.md` calls "mandatory") that
  `cleanup()` actually unregisters the payment-result `BroadcastReceiver` (verified by sending a
  real broadcast before/after `cleanup()` and checking delivery). Uses
  [`TestCheckoutActivity`/`PlainTestActivity`](src/androidTest/java/tech/nimbbl/webviewsdk/core/TestCheckoutActivity.kt),
  declared in `src/androidTest/AndroidManifest.xml` with their own `MAIN`/`LAUNCHER` intent-filters
  (needed for `ActivityScenario.launch(Class)` to resolve them in this self-instrumenting test APK).
- [`ValidationUtilsTest`](src/androidTest/java/tech/nimbbl/webviewsdk/utils/ValidationUtilsTest.kt),
  [`PaymentHandlerTest`](src/androidTest/java/tech/nimbbl/webviewsdk/utils/PaymentHandlerTest.kt),
  [`UPIManagerTest`](src/androidTest/java/tech/nimbbl/webviewsdk/utils/UPIManagerTest.kt),
  [`WebViewManagerTest`](src/androidTest/java/tech/nimbbl/webviewsdk/utils/WebViewManagerTest.kt)
  — the rest of `utils`.
- [`NimbblCheckoutMainActivityTest`](src/androidTest/java/tech/nimbbl/webviewsdk/ui/activity/NimbblCheckoutMainActivityTest.kt)
  — `onCreate`'s checkout-initiation path: missing-options validation, and the real `updateOrder()`
  network round-trip (success + failure) against [`LocalHttpServer`](src/androidTest/java/tech/nimbbl/webviewsdk/testutil/LocalHttpServer.kt)
  (a loopback `ServerSocket`-based fake server — see `core_api_sdk/TESTING.md`'s "Mocking the HTTP
  layer" section for why this exists instead of a mock). Deliberately stops short of asserting on
  the subsequent WebView page load — that goes through the OS's own Chromium network stack, not
  our code, and would be the flakiest possible thing to assert on. Needs a `Theme.AppCompat`
  application theme to launch at all (a real host app supplies one; this standalone test APK
  doesn't), added to `src/androidTest/AndroidManifest.xml` test-only.

## Coverage

```bash
# Instrumented test coverage report (requires a connected device/emulator;
# root-scoped with `:` for the same reason as above)
./gradlew :createDebugAndroidTestCoverageReport
# → build/reports/coverage/androidTest/debug/connected/index.html

# Unit test coverage report
./gradlew :createDebugUnitTestCoverageReport
# → build/reports/coverage/test/debug/index.html
```

As of this writing: `models` ~98% lines, `core` ~66%, `ui.activity` ~54%, `utils` ~55%. Overall
~49% instruction / ~53% line coverage (up from 7% before this module had any real tests).

## Lint

```bash
./gradlew :lint
```
