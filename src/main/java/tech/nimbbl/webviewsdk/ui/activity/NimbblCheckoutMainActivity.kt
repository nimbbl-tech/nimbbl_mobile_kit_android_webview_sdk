package tech.nimbbl.webviewsdk.ui.activity

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.coroutines.CoroutineScope
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
    private lateinit var transactionID: String
    private var orderID = ""
    private var subMerchantId = ""
    private var paymentUrl = ""
    private var isActivityDestroyed = false

    // Manager classes
    private lateinit var webViewManager: WebViewManager
    private lateinit var upiManager: UPIManager
    private lateinit var paymentHandler: PaymentHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nimbbl_checkout_main)
        
        // Enable edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootView = findViewById<android.widget.FrameLayout>(R.id.ll_main_activity)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBarsInsets.top,
                bottom = systemBarsInsets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        webView = findViewById(R.id.webview)
        
        // Preload WebView for better performance
        preloadWebView()
        
        // Show loading state IMMEDIATELY to prevent white screen (after webView is initialized)
        showLoadingState()
        
        applyStatusBarStyle(R.color.status_bar_color)
        applyNavigationBarStyle(R.color.navigation_bar_color)

        // Initialize manager classes
        initializeManagers()

        // Set up back navigation handler for all Android versions
        setupBackNavigation()

        // Disable Android gesture back navigation (swipe from edge) after view is ready
        disableGestureBackNavigation()

        if (intent.hasExtra(PaymentConstants.EXTRA_TAG_OPTIONS)) {
            options = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(PaymentConstants.EXTRA_TAG_OPTIONS, NimbblCheckoutOptions::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(PaymentConstants.EXTRA_TAG_OPTIONS)
            }!!
            if (!isNetConnected(this)) {
                hideLoadingState()
                onFailedPayment(getString(R.string.no_internet),false)
            } else {
                orderID = parseJwtToken(this@NimbblCheckoutMainActivity, options.orderToken!!)
                subMerchantId =
                    getMerchantIDFromJwtToken(this@NimbblCheckoutMainActivity, options.orderToken!!)
                SDKUtils.logDebug(PaymentConstants.DEBUG_ORDER_ID_PREFIX, orderID)
                updateOrder(options.orderToken!!, orderID)
            }
        } else {
            hideLoadingState()
            // No options in intent: report failure without touching uninitialized options
            onFailedPayment(getString(R.string.input_sent_invalid), true)
        }
    }

    // Apply status bar background + icon color
    fun Activity.applyStatusBarStyle(colorResId: Int) {
        val color = ContextCompat.getColor(this, colorResId)
        window.statusBarColor = color

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = isUsingLightTheme()
    }

    // Apply navigation bar background + icon color
    fun Activity.applyNavigationBarStyle(colorResId: Int) {
        val color = ContextCompat.getColor(this, colorResId)
        window.navigationBarColor = color

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightNavigationBars = isUsingLightTheme()
    }

    // Check if current theme is light
    fun Context.isUsingLightTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode != Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Set up back navigation handler for all Android versions
     * Uses OnBackPressedDispatcher which works on all Android API levels
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle back button press - inject JS to handle back in WebView
                insertJSToWebview(true)
            }
        })
        SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Back navigation handler set up via OnBackPressedDispatcher")
    }

    /**
     * Disable Android gesture back navigation (swipe from edge)
     * This method is called after the view is properly initialized
     * Note: System gesture exclusion is only available on Android Q (API 29)+
     * For older devices, back navigation is already handled via OnBackPressedDispatcher in setupBackNavigation()
     */
    private fun disableGestureBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                window.systemGestureExclusionRects = listOf(
                    android.graphics.Rect(0, 0, 100, resources.displayMetrics.heightPixels)
                )
                SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Android gesture back navigation disabled")
            } catch (e: Exception) {
                SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "Failed to disable gesture back navigation: ${e.message}")
            }
        } else {
            // On older Android versions, gesture navigation isn't a system feature
            // Back navigation is already handled via OnBackPressedDispatcher in setupBackNavigation()
            SDKUtils.logDebug(PaymentConstants.TAG_MAIN_ACTIVITY, "Gesture exclusion not needed on Android < Q (back handled via OnBackPressedDispatcher)")
        }
    }

    /**
     * Preload WebView for better performance
     */
    private fun preloadWebView() {
        try {
            // Enable hardware acceleration for better performance
            webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            // Preload WebView with optimized settings for faster initialization
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT)
                
                // Additional preload optimizations
                setLoadWithOverviewMode(true)
                setUseWideViewPort(true)
                setBuiltInZoomControls(false)
                setDisplayZoomControls(false)
                
                // Memory optimizations
                setMinimumFontSize(8)
                setMinimumLogicalFontSize(8)
                setTextZoom(100)
                
                // Layout optimizations
                setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)
            }
            
            // Load a minimal HTML page to initialize the WebView engine faster
            val minimalHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { margin: 0; padding: 0; background: white; }
                    </style>
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
        webViewManager = WebViewManager(this, webView)
        upiManager = UPIManager(this)
        paymentHandler = PaymentHandler(this)
        
        // Set callback for when WebView finishes loading
        webViewManager.setOnPageFinishedCallback {
            runOnUiThread {
                // Reduced delay for faster response - WebView is preloaded
                webView.postDelayed({
                    hideLoadingState()
                }, 50) // 50ms delay for content painting (reduced from 100ms)
            }
        }
        
        // Additional performance optimization: Warm up the WebView
        warmUpWebView()
    }
    
    /**
     * Warm up WebView for optimal performance
     */
    private fun warmUpWebView() {
        try {
            // Force WebView to initialize its rendering engine
            webView.post {
                // This helps initialize the WebView's internal components
                webView.evaluateJavascript("console.log('WebView warmed up');", null)
            }
        } catch (e: Exception) {
            SDKUtils.logError(PaymentConstants.TAG_MAIN_ACTIVITY, "Error warming up WebView: ${e.message}", e)
        }
    }

    private fun updateOrder(token: String, orderId: String) {
        try {
            // A hosting SDK (e.g. react-native) can override the surface; otherwise this is android.
            val referrerPlatform = options.referrerPlatform ?: PaymentConstants.PLATFORM_TYPE_ANDROID
            // Log update order payload event
            EventLoggingUtils.safeLogEvent(
                context = this,
                eventName = EventConstants.UPDATE_ORDER_PAYLOAD,
                orderId,
                token,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    "callback_mode" to PaymentConstants.CALLBACK_MODE_MOBILE,
                    "platform_type" to referrerPlatform
                ),
                sdkVersion = PaymentConstants.SDK_VERSION
            )

            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    NimbblCoreApiSDK.getInstance()
                        ?.updateOrder(
                            token,
                            orderId,
                            PaymentConstants.CALLBACK_MODE_MOBILE,
                            referrerPlatform
                        )
                }
                if (result != null && result.isSuccessful) {
                    setupWebView()
                } else {
                    if (result != null && !result.rawBody.isNullOrEmpty()) {
                        try {
                            val jObjError = JSONObject(result.rawBody!!)
                            if (jObjError.has(PaymentConstants.JSON_KEY_ERROR)) {
                                val errorObj = jObjError.getJSONObject(PaymentConstants.JSON_KEY_ERROR)
                                onFailedPayment(
                                    errorObj.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE),
                                    true
                                )
                            } else {
                                onFailedPayment(getString(R.string.error_in_update_order), true)
                            }
                        } catch (e: Exception) {
                            onFailedPayment(getString(R.string.error_in_update_order), true)
                        }
                    } else {
                        onFailedPayment(getString(R.string.error_in_update_order), true)
                    }
                }
            }
        } catch (e: Exception) {
            onFailedPayment(e.toString(), true)
        }
        NimbblCheckoutSDK.getInstance().setMyActivityWeakReference(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Build payment URL using the URL builder
        val baseUrl = String.format(RestApiUtils.WEB_VIEW_VIEW_URL, options.orderToken)
        paymentUrl = PaymentURLBuilder.buildPaymentURL(baseUrl, options)

        // Setup WebView using the manager
        webViewManager.setupWebView(paymentUrl, orderID, options.orderToken!!, subMerchantId)

        // Add JavaScript interface directly to ensure proper annotations
        webView.addJavascriptInterface(
            INimmbleWebViewInterface(this),
            PaymentConstants.JAVASCRIPT_INTERFACE_NAME
        )
        
        // Don't hide loading state here - wait for WebView to actually finish loading
        // The loading state will be hidden in onPageFinished callback
    }

    inner class INimmbleWebViewInterface(private val mContext: Context) {

        @JavascriptInterface
        fun openUpiIntent(parameters: String) {
            SDKUtils.logDebug(PaymentConstants.TAG_SAN, "openUpiIntent-->$parameters")

            runOnUiThread {
                try {
                    // Check if activity is destroyed before proceeding
                    if (isActivityDestroyed()) {
                        SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "Activity is destroyed, ignoring UPI intent")
                        return@runOnUiThread
                    }

                    // Validate UPI parameters (single validation call)
                    if (!upiManager.validateUPIParameters(
                            parameters, orderID,
                            options.orderToken.toString(), subMerchantId
                        )
                    ) {
                        SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "Invalid UPI parameters received")
                        insertJSToWebview(false)
                        return@runOnUiThread
                    }

                    val responseObj = JSONObject(parameters)
                    val url = responseObj.getString(PaymentConstants.UPI_PARAM_URL)
                    val packageName = responseObj.getString(PaymentConstants.UPI_PARAM_PACKAGE_NAME)
                    transactionID = responseObj.getString(PaymentConstants.UPI_PARAM_TRANSACTION_ID)

                    // Log UPI intent started event
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

                    // Add intent resolution check
                    if (intent.resolveActivity(packageManager) != null) {
                        resultLauncher.launch(intent)
                    } else {
                        SDKUtils.logWarning(PaymentConstants.TAG_MAIN_ACTIVITY, "No app found to handle UPI intent for package: $packageName")
                        Toast.makeText(
                            this@NimbblCheckoutMainActivity,
                            "No UPI app found to handle this request",
                            Toast.LENGTH_SHORT
                        ).show()
                        insertJSToWebview(false)
                    }
                } catch (e: Exception) {
                    // Log exception event
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

                    Toast.makeText(
                        this@NimbblCheckoutMainActivity,
                        PaymentConstants.ERROR_UNABLE_TO_LAUNCH_UPI,
                        Toast.LENGTH_SHORT
                    ).show()
                    insertJSToWebview(false)
                }
            }
        }
    }


    private fun insertJSToWebview(isDeviceBackPressed: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isDeviceBackPressed) {
                    handleDeviceBackPressed()
                } else {
                    handleUpiIntentBack()
                }
            } catch (e: Exception) {
                SDKUtils.logError(PaymentConstants.TAG_BACK_HANDLER, "Error handling back press: ${e.message}", e)
                // Log exception event
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
        val currentUrl = webViewManager.getCurrentUrl()

        SDKUtils.logDebug(PaymentConstants.TAG_BACK_HANDLER, "currentUrl: $currentUrl")
        SDKUtils.logDebug(PaymentConstants.TAG_BACK_HANDLER, "expected url: $paymentUrl")

        if (!currentUrl.isNullOrEmpty()) {
            val uri = currentUrl.toUri()
            val baseUrl = "${uri.scheme}://${uri.authority}"

            SDKUtils.logDebug(PaymentConstants.DEBUG_BASE_URL_PREFIX, baseUrl)

            val expectedBase =
                paymentUrl.toUri().buildUpon().path("").encodedQuery(null).fragment(null).build()
                    .toString()

            if (!baseUrl.startsWith(expectedBase)) {
                val updatedUrl = PaymentURLBuilder.buildBrowserBackURL(
                    this@NimbblCheckoutMainActivity,
                    paymentUrl,
                    currentUrl,
                    orderID,
                    options.orderToken,
                    subMerchantId
                )
                webViewManager.loadUrl(updatedUrl, orderID, options.orderToken, subMerchantId)
            }
        }

        val jsonObject = JSONObject().apply {
            put(PayloadKeys.key_sdk_action, PaymentConstants.SDK_ACTION_DEVICE_BACK_INITIATED)
            put(PayloadKeys.key_source, PaymentConstants.SDK_SOURCE_ANDROID)
        }

        // Log back button pressed event
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
        webViewManager.evaluateJavaScript("${PaymentConstants.JS_FUNCTION_POST_MESSAGE}($jsString, '*');")
    }

    private suspend fun handleUpiIntentBack() {
        val paymentEnquiryResponse = withContext(Dispatchers.IO) {
            NimbblCoreApiSDK.getInstance()?.getTransactionEnquiry(
                options.orderToken!!,
                orderID,
                orderID,
                transactionID
            )
        }

        val jsonObject = JSONObject().apply {
            put(
                PayloadKeys.key_upi_intent_back_response,
                PaymentConstants.UPI_INTENT_BACK_RESPONSE_CLOSE
            )
            val status = if (paymentEnquiryResponse?.isSuccessful == true) {
                paymentEnquiryResponse.data?.order?.status ?: ""
            } else {
                ""
            }
            put(PayloadKeys.key_transaction_enquiry_api_response, status)
        }

        val jsString = JSONObject.quote(jsonObject.toString())
        webViewManager.evaluateJavaScript("${PaymentConstants.JS_FUNCTION_POST_MESSAGE}($jsString, '*');")
    }


    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            SDKUtils.logDebug(PaymentConstants.TAG_SAN, "resultCode-->" + result.resultCode + "data-->" + result.data)

            // Log UPI intent closed event
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

    // UPI intents are now handled by WebViewManager
    // They are automatically sent only once when the initial page loads
    // Use webViewManager.triggerUpiIntents() if manual triggering is needed

    // WebView client is now handled by WebViewManager
    // Payment handling is now handled by PaymentHandler

    private fun onFailedPayment(errorStr: String,isSendEvent:Boolean) {
        paymentHandler.handlePaymentFailure(
            orderID,
            options.orderToken!!,
            subMerchantId = subMerchantId,
            errorStr,
            isSendEvent
        )
    }


    override fun onResume() {
        super.onResume()
    }

    /**
     * Check if activity is destroyed
     * @return true if activity is destroyed, false otherwise
     */
    private fun isActivityDestroyed(): Boolean {
        return isActivityDestroyed || isFinishing || isDestroyed
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityDestroyed = true

        // Clean up WebView resources
        try {
            webViewManager.destroy()
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error destroying WebView manager: ${e.message}", e)
        }

        // Clean up SDK resources
        try {
            NimbblCheckoutSDK.getInstance().cleanup()
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error cleaning up SDK: ${e.message}", e)
        }
    }

    // Bitmap conversion is now handled by UPIManager

    /**
     * Show loading state immediately to prevent white screen
     */
    private fun showLoadingState() {
        try {
            // Find shimmer view
            val shimmerView = findViewById<android.view.View>(R.id.shimmer_layout)
            
            // Show shimmer layout
            shimmerView?.visibility = android.view.View.VISIBLE
            
            // Hide WebView initially
            webView.visibility = android.view.View.GONE
            
            // Start shimmer animation
            if (shimmerView != null) {
                startShimmerAnimation(shimmerView)
            }
            
            SDKUtils.logDebug(TAG, "Shimmer loading state shown")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error showing loading state: ${e.message}", e)
        }
    }

    /**
     * Hide loading state when WebView is ready
     */
    private fun hideLoadingState() {
        try {
            // Find shimmer view
            val shimmerView = findViewById<android.view.View>(R.id.shimmer_layout)
            
            // Stop shimmer animation
            if (shimmerView != null) {
                shimmerView.clearAnimation()
            }
            
            // Hide shimmer with smooth transition
            shimmerView?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                shimmerView.visibility = android.view.View.GONE
                showWebView()
            }
            
            SDKUtils.logDebug(TAG, "Shimmer loading state hidden")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error hiding loading state: ${e.message}", e)
            // Fallback: just show WebView
            showWebView()
        }
    }

    /**
     * Show WebView with smooth transition
     */
    private fun showWebView() {
        try {
            webView.visibility = android.view.View.VISIBLE
            webView.alpha = 0f
            webView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
                
            SDKUtils.logDebug(TAG, "WebView shown with animation")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error showing WebView: ${e.message}", e)
            webView.visibility = android.view.View.VISIBLE
        }
    }

    /**
     * Start shimmer animation for loading effect
     */
    private fun startShimmerAnimation(view: android.view.View) {
        try {
            // Find all shimmer placeholder views and animate them individually
            val shimmerViews = mutableListOf<android.view.View>()
            findShimmerViews(view, shimmerViews)
            
            // Animate each shimmer view with a slight delay for wave effect
            shimmerViews.forEachIndexed { index, shimmerView ->
                val delay = index * 100L // 100ms delay between each item
                shimmerView.postDelayed({
                    animateShimmerItem(shimmerView)
                }, delay)
            }
            
            SDKUtils.logDebug(TAG, "Shimmer animation started for ${shimmerViews.size} items")
        } catch (e: Exception) {
            SDKUtils.logError(TAG, "Error starting shimmer animation: ${e.message}", e)
        }
    }

    /**
     * Recursively find all shimmer placeholder views
     */
    private fun findShimmerViews(parent: android.view.View, shimmerViews: MutableList<android.view.View>) {
        if (parent is android.view.ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                if (child.background != null && child.background.constantState != null) {
                    // Check if this is a shimmer placeholder (has gray background)
                    shimmerViews.add(child)
                }
                findShimmerViews(child, shimmerViews)
            }
        }
    }

    /**
     * Animate individual shimmer item
     */
    private fun animateShimmerItem(view: android.view.View) {
        if (view.visibility == android.view.View.VISIBLE) {
            val shimmerRunnable = object : Runnable {
                override fun run() {
                    if (view.visibility == android.view.View.VISIBLE) {
                        view.animate()
                            .alpha(0.3f)
                            .setDuration(1200)
                            .withEndAction {
                                if (view.visibility == android.view.View.VISIBLE) {
                                    view.animate()
                                        .alpha(0.8f)
                                        .setDuration(1200)
                                        .withEndAction {
                                            if (view.visibility == android.view.View.VISIBLE) {
                                                view.postDelayed(this, 200) // Small delay before next cycle
                                            }
                                        }
                                        .start()
                                }
                            }
                            .start()
                    }
                }
            }
            view.post(shimmerRunnable)
        }
    }

}
