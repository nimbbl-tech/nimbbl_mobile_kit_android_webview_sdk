package tech.nimbbl.webviewsdk.ui.activity

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tech.nimbbl.coreapisdk.api.RestApiUtils
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.core.constants.PayloadKeys
import tech.nimbbl.coreapisdk.utils.extensions.getMerchantIDFromJwtToken
import tech.nimbbl.coreapisdk.utils.extensions.isNetConnected
import tech.nimbbl.coreapisdk.utils.extensions.parseJwtToken
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.R
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.core.NimbblCheckoutSDK
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.utils.PaymentHandler
import tech.nimbbl.webviewsdk.utils.PaymentURLBuilder
import tech.nimbbl.webviewsdk.utils.SDKUtils
import tech.nimbbl.webviewsdk.utils.UPIManager
import tech.nimbbl.webviewsdk.utils.WebViewManager


class NimbblCheckoutMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = PaymentConstants.TAG_MAIN_ACTIVITY
    }

    private lateinit var webView: WebView
    private lateinit var options: NimbblCheckoutOptions
    private var transactionID = ""
    private var orderID = ""
    private var orderToken = ""
    private var subMerchantId = ""
    private var paymentUrl = ""
    private var isShimmerActive = false
    private val shimmerRunnables = mutableMapOf<View, Runnable>()

    // Manager classes
    private lateinit var webViewManager: WebViewManager
    private lateinit var paymentHandler: PaymentHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nimbbl_checkout_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootView = findViewById<FrameLayout>(R.id.ll_main_activity)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBarsInsets.top, bottom = systemBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        webView = findViewById(R.id.webview)

        preloadWebView()
        showLoadingState()
        applyStatusBarStyle(R.color.status_bar_color)
        applyNavigationBarStyle(R.color.navigation_bar_color)
        initializeManagers()
        setupBackNavigation()
        disableGestureBackNavigation()

        if (intent.hasExtra(PaymentConstants.EXTRA_TAG_OPTIONS)) {
            options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(PaymentConstants.EXTRA_TAG_OPTIONS, NimbblCheckoutOptions::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(PaymentConstants.EXTRA_TAG_OPTIONS)
            } ?: run {
                hideLoadingState()
                onFailedPayment(getString(R.string.input_sent_invalid), true)
                return
            }
            val token = options.orderToken ?: run {
                hideLoadingState()
                onFailedPayment(getString(R.string.input_sent_invalid), true)
                return
            }
            orderToken = token
            if (!isNetConnected(this)) {
                hideLoadingState()
                onFailedPayment(getString(R.string.no_internet), false)
            } else {
                try {
                    orderID = parseJwtToken(this, orderToken)
                    subMerchantId = getMerchantIDFromJwtToken(this, orderToken)
                } catch (e: Exception) {
                    SDKUtils.logError(TAG, "Failed to parse order token: ${e.message}", e)
                    hideLoadingState()
                    onFailedPayment(getString(R.string.input_sent_invalid), true)
                    return
                }
                SDKUtils.logDebug(PaymentConstants.DEBUG_ORDER_ID_PREFIX, orderID)
                updateOrder(orderToken, orderID)
            }
        } else {
            hideLoadingState()
            onFailedPayment(getString(R.string.input_sent_invalid), true)
        }
    }

    private fun applyStatusBarStyle(colorResId: Int) {
        val color = ContextCompat.getColor(this, colorResId)
        window.statusBarColor = color
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isUsingLightTheme()
    }

    private fun applyNavigationBarStyle(colorResId: Int) {
        val color = ContextCompat.getColor(this, colorResId)
        window.navigationBarColor = color
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = isUsingLightTheme()
    }

    private fun isUsingLightTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode != Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                insertJSToWebview(true)
            }
        })
        SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Back navigation handler set up via OnBackPressedDispatcher")
    }

    private fun disableGestureBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                window.systemGestureExclusionRects = listOf(
                    Rect(0, 0, 100, resources.displayMetrics.heightPixels)
                )
                SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Android gesture back navigation disabled")
            } catch (e: Exception) {
                SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "Failed to disable gesture back navigation: ${e.message}")
            }
        } else {
            SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Gesture exclusion not needed on Android < Q (back handled via OnBackPressedDispatcher)")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun preloadWebView() {
        try {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT)
                setLoadWithOverviewMode(true)
                setUseWideViewPort(true)
                setBuiltInZoomControls(false)
                setDisplayZoomControls(false)
                setMinimumFontSize(8)
                setMinimumLogicalFontSize(8)
                setTextZoom(100)
            }
            webView.settings.apply {
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION") setAllowFileAccessFromFileURLs(false)
                @Suppress("DEPRECATION") setAllowUniversalAccessFromFileURLs(false)
                setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }
            val minimalHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>body { margin: 0; padding: 0; background: white; }</style>
                </head>
                <body></body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, minimalHtml, "text/html", "utf-8", null)
        } catch (e: Exception) {
            SDKUtils.logError(PaymentConstants.TAG_MAIN_ACTIVITY, "Error preloading WebView: ${e.message}", e)
        }
    }

    private fun initializeManagers() {
        paymentHandler = PaymentHandler(applicationContext)
        webViewManager = WebViewManager(applicationContext, webView, paymentHandler)

        webViewManager.setOnPageFinishedCallback {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    webView.postDelayed({
                        if (!isFinishing && !isDestroyed) hideLoadingState()
                    }, 50)
                }
            }
        }

        warmUpWebView()
    }

    private fun warmUpWebView() {
        try {
            webView.post {
                webView.evaluateJavascript("console.log('WebView warmed up');", null)
            }
        } catch (e: Exception) {
            SDKUtils.logError(PaymentConstants.TAG_MAIN_ACTIVITY, "Error warming up WebView: ${e.message}", e)
        }
    }

    private fun updateOrder(token: String, orderId: String) {
        NimbblCheckoutSDK.getInstance().setMyActivityWeakReference(this)
        try {
            EventLoggingUtils.safeLogEvent(
                context = this,
                eventName = EventConstants.UPDATE_ORDER_PAYLOAD,
                orderId,
                token,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    "callback_mode" to PaymentConstants.CALLBACK_MODE_MOBILE,
                    "platform_type" to PaymentConstants.PLATFORM_TYPE_ANDROID
                ),
                sdkVersion = PaymentConstants.SDK_VERSION
            )

            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        NimbblCoreApiSDK.getInstance()
                            .updateOrder(token, orderId, PaymentConstants.CALLBACK_MODE_MOBILE, PaymentConstants.PLATFORM_TYPE_ANDROID)
                    }
                    if (result != null && result.isSuccessful) {
                        setupWebView()
                    } else {
                        onFailedPayment(parseUpdateOrderError(result?.rawBody), true)
                    }
                } catch (e: Exception) {
                    SDKUtils.logError(TAG, "updateOrder failed: ${e.message}", e)
                    onFailedPayment(e.message ?: getString(R.string.error_in_update_order), true)
                }
            }
        } catch (e: Exception) {
            onFailedPayment(e.message ?: getString(R.string.error_in_update_order), true)
        }
    }

    private fun parseUpdateOrderError(rawBody: String?): String {
        if (rawBody.isNullOrEmpty()) return getString(R.string.error_in_update_order)
        return try {
            val error = JSONObject(rawBody).optJSONObject(PaymentConstants.JSON_KEY_ERROR)
            error?.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE)
                ?: getString(R.string.error_in_update_order)
        } catch (_: Exception) {
            getString(R.string.error_in_update_order)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        try {
            val baseUrl = String.format(RestApiUtils.WEB_VIEW_VIEW_URL, orderToken)
            paymentUrl = PaymentURLBuilder.buildPaymentURL(baseUrl, options)
        } catch (e: IllegalArgumentException) {
            SDKUtils.logError(TAG, "Invalid payment URL: ${e.message}", e)
            onFailedPayment(e.message ?: getString(R.string.input_sent_invalid), true)
            return
        }
        webViewManager.setupWebView(paymentUrl, orderID, orderToken, subMerchantId)
        webView.addJavascriptInterface(INimmbleWebViewInterface(this), PaymentConstants.JAVASCRIPT_INTERFACE_NAME)
    }

    private inner class INimmbleWebViewInterface(private val mContext: Context) {

        @Suppress("unused")
        @JavascriptInterface
        fun openUpiIntent(parameters: String) {
            SDKUtils.logDebug(TAG, "openUpiIntent-->$parameters")

            runOnUiThread {
                try {
                    if (isFinishing || isDestroyed) {
                        SDKUtils.logWarning(TAG, "Activity is destroyed, ignoring UPI intent")
                        return@runOnUiThread
                    }

                    if (!UPIManager(this@NimbblCheckoutMainActivity).validateUPIParameters(parameters, orderID, options.orderToken.toString(), subMerchantId)) {
                        SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "Invalid UPI parameters received")
                        insertJSToWebview(false)
                        return@runOnUiThread
                    }

                    val responseObj = JSONObject(parameters)
                    val url = responseObj.getString(PaymentConstants.UPI_PARAM_URL)
                    val packageName = responseObj.getString(PaymentConstants.UPI_PARAM_PACKAGE_NAME)
                    transactionID = responseObj.getString(PaymentConstants.UPI_PARAM_TRANSACTION_ID)

                    EventLoggingUtils.safeLogEvent(
                        mContext,
                        eventName = EventConstants.UPI_INTENT_STARTED,
                        orderId = orderID,
                        options.orderToken,
                        subMerchantId,
                        additionalData = mapOf<String, Any>(
                            "package_name" to packageName,
                            "transactionId" to transactionID,
                            "upi_url" to url
                        ),
                        sdkVersion = PaymentConstants.SDK_VERSION
                    )

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    intent.setPackage(packageName)

                    if (intent.resolveActivity(packageManager) != null) {
                        resultLauncher.launch(intent)
                    } else {
                        SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "No app found to handle UPI intent for package: $packageName")
                        Toast.makeText(this@NimbblCheckoutMainActivity, PaymentConstants.ERROR_NO_UPI_APP_FOUND, Toast.LENGTH_SHORT).show()
                        insertJSToWebview(false)
                    }
                } catch (e: Exception) {
                    EventLoggingUtils.safeLogEvent(
                        mContext,
                        eventName = EventConstants.EXCEPTION_THROWN,
                        orderId = orderID,
                        options.orderToken,
                        subMerchantId,
                        additionalData = mapOf<String, Any>(
                            "error" to (e.message ?: "Unknown error"),
                            "location" to "UPI intent handling",
                            "package_name" to "unknown"
                        ),
                        sdkVersion = PaymentConstants.SDK_VERSION
                    )
                    Toast.makeText(this@NimbblCheckoutMainActivity, PaymentConstants.ERROR_UNABLE_TO_LAUNCH_UPI, Toast.LENGTH_SHORT).show()
                    insertJSToWebview(false)
                }
            }
        }
    }

    private fun insertJSToWebview(isDeviceBackPressed: Boolean) {
        lifecycleScope.launch {
            try {
                if (isDeviceBackPressed) handleDeviceBackPressed() else handleUpiIntentBack()
            } catch (e: Exception) {
                SDKUtils.logError(PaymentConstants.TAG_BACK_HANDLER, "Error handling back press: ${e.message}", e)
                EventLoggingUtils.safeLogEvent(
                    context = this@NimbblCheckoutMainActivity,
                    eventName = EventConstants.EXCEPTION_THROWN,
                    orderId = orderID,
                    options.orderToken,
                    subMerchantId,
                    additionalData = mapOf<String, Any>(
                        "error" to (e.message ?: "Unknown error"),
                        "location" to "back press handling"
                    ),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )
            }
        }
    }

    private fun handleDeviceBackPressed() {
        if (paymentUrl.isEmpty()) return
        val currentUrl = webViewManager.getCurrentUrl()

        SDKUtils.logDebug(PaymentConstants.TAG_BACK_HANDLER, "currentUrl: $currentUrl")
        SDKUtils.logDebug(PaymentConstants.TAG_BACK_HANDLER, "expected url: $paymentUrl")

        if (!currentUrl.isNullOrEmpty()) {
            val uri = currentUrl.toUri()
            val scheme = uri.scheme
            val authority = uri.authority
            if (scheme != null && authority != null) {
                val baseUrl = "$scheme://$authority"
                SDKUtils.logDebug(PaymentConstants.DEBUG_BASE_URL_PREFIX, baseUrl)

                val expectedBase = paymentUrl.toUri().buildUpon().path("").encodedQuery(null).fragment(null).build().toString()

                if (!baseUrl.startsWith(expectedBase)) {
                    val updatedUrl = PaymentURLBuilder.buildBrowserBackURL(
                        this, paymentUrl, currentUrl, orderID, options.orderToken, subMerchantId
                    )
                    webViewManager.loadUrl(updatedUrl, orderID, options.orderToken, subMerchantId)
                }
            }
        }

        val jsonObject = JSONObject().apply {
            put(PayloadKeys.key_sdk_action, PaymentConstants.SDK_ACTION_DEVICE_BACK_INITIATED)
            put(PayloadKeys.key_source, PaymentConstants.SDK_SOURCE_ANDROID)
        }

        EventLoggingUtils.safeLogEvent(
            context = this,
            eventName = EventConstants.BACK_BUTTON_PRESSED,
            orderId = orderID,
            options.orderToken,
            subMerchantId,
            additionalData = mapOf<String, Any>("current_url" to (currentUrl ?: "unknown")),
            sdkVersion = PaymentConstants.SDK_VERSION
        )

        val jsString = JSONObject.quote(jsonObject.toString())
        val checkoutOrigin = runCatching { paymentUrl.toUri().run { "$scheme://$host" } }.getOrElse { "*" }
        webViewManager.evaluateJavaScript("${PaymentConstants.JS_FUNCTION_POST_MESSAGE}($jsString, '$checkoutOrigin');")
    }

    private suspend fun handleUpiIntentBack() {
        val paymentEnquiryResponse = withContext(Dispatchers.IO) {
            NimbblCoreApiSDK.getInstance().getTransactionEnquiry(orderToken, orderID, "", transactionID)
        }

        val jsonObject = JSONObject().apply {
            put(PayloadKeys.key_upi_intent_back_response, PaymentConstants.UPI_INTENT_BACK_RESPONSE_CLOSE)
            val status = if (paymentEnquiryResponse?.isSuccessful == true) {
                paymentEnquiryResponse.data?.order?.status ?: ""
            } else ""
            put(PayloadKeys.key_transaction_enquiry_api_response, status)
        }

        val jsString = JSONObject.quote(jsonObject.toString())
        val checkoutOrigin = runCatching { paymentUrl.toUri().run { "$scheme://$host" } }.getOrElse { "*" }
        webViewManager.evaluateJavaScript("${PaymentConstants.JS_FUNCTION_POST_MESSAGE}($jsString, '$checkoutOrigin');")
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        SDKUtils.logDebug(TAG, "resultCode-->" + result.resultCode + "data-->" + result.data)

        EventLoggingUtils.safeLogEvent(
            context = this,
            eventName = EventConstants.UPI_INTENT_CLOSED,
            orderId = orderID,
            options.orderToken,
            subMerchantId,
            additionalData = mapOf<String, Any>(
                "result_code" to result.resultCode.toString(),
                "result_data" to (result.data?.toString() ?: "null"),
                "transactionId" to transactionID
            ),
            sdkVersion = PaymentConstants.SDK_VERSION
        )
        insertJSToWebview(false)
    }

    private fun onFailedPayment(errorStr: String, isSendEvent: Boolean) {
        if (!::paymentHandler.isInitialized) return
        paymentHandler.handlePaymentFailure(orderID, orderToken, subMerchantId = subMerchantId, errorStr, isSendEvent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isShimmerActive = false
        shimmerRunnables.forEach { (view, runnable) -> view.removeCallbacks(runnable) }
        shimmerRunnables.clear()
        try {
            webViewManager.destroy()
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error destroying WebView manager: ${e.message}", e)
        }
        try {
            NimbblCheckoutSDK.getInstance().cleanup()
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error cleaning up SDK: ${e.message}", e)
        }
    }

    private fun showLoadingState() {
        try {
            val shimmerView = findViewById<View>(R.id.shimmer_layout)
            shimmerView?.visibility = View.VISIBLE
            webView.visibility = View.GONE
            if (shimmerView != null) {
                isShimmerActive = true
                startShimmerAnimation(shimmerView)
            }
            SDKUtils.logDebug(TAG, "Shimmer loading state shown")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error showing loading state: ${e.message}", e)
        }
    }

    private fun hideLoadingState() {
        try {
            val shimmerView = findViewById<View>(R.id.shimmer_layout)
            isShimmerActive = false
            shimmerRunnables.forEach { (view, runnable) -> view.removeCallbacks(runnable) }
            shimmerRunnables.clear()
            shimmerView?.clearAnimation()
            shimmerView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                shimmerView.visibility = View.GONE
                showWebView()
            }
            SDKUtils.logDebug(TAG, "Shimmer loading state hidden")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error hiding loading state: ${e.message}", e)
            showWebView()
        }
    }

    private fun showWebView() {
        try {
            webView.visibility = View.VISIBLE
            webView.alpha = 0f
            webView.animate().alpha(1f).setDuration(300).start()
            SDKUtils.logDebug(TAG, "WebView shown with animation")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error showing WebView: ${e.message}", e)
            webView.visibility = View.VISIBLE
        }
    }

    private fun startShimmerAnimation(view: View) {
        try {
            val shimmerViews = mutableListOf<View>()
            findShimmerViews(view, shimmerViews)
            shimmerViews.forEachIndexed { index, shimmerView ->
                val delay = index * 100L
                val staggerRunnable = Runnable { if (isShimmerActive) animateShimmerItem(shimmerView) }
                shimmerRunnables[shimmerView] = staggerRunnable
                shimmerView.postDelayed(staggerRunnable, delay)
            }
            SDKUtils.logDebug(TAG, "Shimmer animation started for ${shimmerViews.size} items")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error starting shimmer animation: ${e.message}", e)
        }
    }

    private fun findShimmerViews(parent: View, shimmerViews: MutableList<View>) {
        if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                if (child.background != null && child.background.constantState != null) {
                    shimmerViews.add(child)
                }
                findShimmerViews(child, shimmerViews)
            }
        }
    }

    private fun animateShimmerItem(view: View) {
        if (view.visibility != View.VISIBLE) return
        @Suppress("ObjectLiteralToLambda") // self-reference via `this` inside postDelayed requires object form
        val shimmerRunnable = object : Runnable {
            override fun run() {
                if (!isShimmerActive || view.visibility != View.VISIBLE) return
                view.animate()
                    .alpha(0.3f)
                    .setDuration(1200)
                    .withEndAction {
                        if (!isShimmerActive || view.visibility != View.VISIBLE) return@withEndAction
                        view.animate()
                            .alpha(0.8f)
                            .setDuration(1200)
                            .withEndAction {
                                if (isShimmerActive && view.visibility == View.VISIBLE) {
                                    view.postDelayed(this, 200)
                                }
                            }
                            .start()
                    }
                    .start()
            }
        }
        shimmerRunnables[view] = shimmerRunnable
        view.post(shimmerRunnable)
    }

}
