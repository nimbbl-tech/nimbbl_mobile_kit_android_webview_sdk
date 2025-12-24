package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled

/**
 * Manager class for handling WebView operations and configuration
 */
class WebViewManager(
    private val context: Context,
    private val webView: WebView
) {
    
    companion object {
        private const val TAG = "WebViewManager"
        private const val ERROR_SSL_HANDSHAKE = 104
    }
    
    private var isInitialPageLoaded = false
    private var isWebViewLoadedEventLogged = false
    private var isDestroyed = false
    private var onPageFinishedCallback: (() -> Unit)? = null
    private var hasCallbackBeenInvoked = false
    
    /**
     * Set callback for when page finishes loading
     * @param callback Callback function to execute
     */
    fun setOnPageFinishedCallback(callback: () -> Unit) {
        onPageFinishedCallback = callback
    }

    /**
     * Setup WebView with proper configuration
     * @param url URL to load
     * @param orderID Order ID for logging
     * @param orderToken Order token for logging
     */
    fun setupWebView(url: String, orderID: String, orderToken: String,subMerchantId:String) {
        // Reset initial page loaded flag
        isInitialPageLoaded = false
        // Reset webview loaded event flag
        isWebViewLoadedEventLogged = false
        // Reset callback invocation flag
        hasCallbackBeenInvoked = false
        
        // Log webview launch event
        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.WEBVIEW_LAUNCH,
            orderId = orderID,
            orderToken,
            subMerchantId,
            additionalData = mapOf("url" to url),
            sdkVersion = PaymentConstants.SDK_VERSION
        )
        
        // Configure WebView settings first for better performance
        configureWebViewSettings()
        setupWebViewClient(orderID, orderToken,subMerchantId)
        setupWebChromeClient()
        
        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error loading URL: ${e.message}", e)
            }
            // Log webview failed event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.WEBVIEW_FAILED,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    "error_code" to "URL_LOAD_ERROR",
                    "error_description" to (e.message ?: "Unknown error"),
                    "url" to url
                ),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
        }
    }
    
    /**
     * Configure WebView settings with security enhancements
     */
    private fun configureWebViewSettings() {
        webView.settings.apply {
            // Basic functionality settings
            domStorageEnabled = true
            javaScriptEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            
            // Performance optimizations for faster rendering
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT)
            
            // Optimize for mobile
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            
            // Network optimizations
            blockNetworkImage = false
            blockNetworkLoads = false
            
            // Additional performance optimizations
            mediaPlaybackRequiresUserGesture = false
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
            
            // Memory optimizations
            minimumFontSize = 8
            minimumLogicalFontSize = 8
            
            // Text rendering optimizations
            textZoom = 100
            
            // Layout optimizations
            layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            
            // Security enhancements
            allowFileAccess = false
            allowContentAccess = false
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
            
            // Mixed content security
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            }
            
            // Safe browsing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            
            // Additional security settings
            setGeolocationEnabled(false)
            databaseEnabled = false
        }
        
        // Enable hardware acceleration on the WebView itself
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Setup WebView client for handling navigation and errors
     */
    private fun setupWebViewClient(orderID: String, orderToken: String,subMerchantId:String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                
                // Enhanced error handling with specific error types
                val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error?.errorCode ?: -1
                } else {
                    -1
                }
                
                val errorDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error?.description?.toString() ?: "Unknown error"
                } else {
                    "Unknown error"
                }
                
                val requestUrl = request?.url?.toString() ?: "unknown"
                
                // Handle specific error types
                when (errorCode) {
                    ERROR_HOST_LOOKUP -> {
                        Log.e(TAG, "Network error: Host lookup failed for URL: $requestUrl")
                        handleNetworkError()
                    }
                    ERROR_TIMEOUT -> {
                        Log.e(TAG, "Network error: Timeout for URL: $requestUrl")
                        handleTimeoutError()
                    }
                    ERROR_CONNECT -> {
                        Log.e(TAG, "Network error: Connection failed for URL: $requestUrl")
                        handleConnectionError()
                    }
                    ERROR_SSL_HANDSHAKE -> {
                        Log.e(TAG, "SSL error: Handshake failed for URL: $requestUrl")
                        handleSSLError()
                    }
                    else -> {
                        Log.e(TAG, "WebView error: $errorDescription (Code: $errorCode) for URL: $requestUrl")
                        handleGenericError(errorCode, errorDescription)
                    }
                }
                
                // Log webview failed event
                EventLoggingUtils.safeLogEvent(
                    context,
                    eventName = EventConstants.WEBVIEW_FAILED,
                    orderId = orderID,
                    orderToken,
                    subMerchantId,
                    additionalData = mapOf<String, Any>(
                        "error_code" to errorCode.toString(),
                        "error_description" to errorDescription,
                        "url" to requestUrl
                    ),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (is_debug_enabled) {
                    Log.d(TAG, "Page started loading: ${url ?: "empty"}")
                }
            }
            
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                // Optimize resource loading for better performance
                val url = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.url?.toString()
                } else {
                    null
                }
                
                if (is_debug_enabled && url != null) {
                    Log.d(TAG, "Intercepting request: $url")
                }
                
                return super.shouldInterceptRequest(view, request)
            }
            
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (is_debug_enabled) {
                    Log.d(TAG, "URL history updated: ${url ?: "empty"} -> isReload: $isReload")
                }
                
                // Log URL intercepted event
                if (url != null) {
                    EventLoggingUtils.safeLogEvent(
                        context,
                        eventName = EventConstants.REDIRECT_URL_HIT,
                        orderId = orderID,
                        orderToken,
                        subMerchantId,
                        additionalData = mapOf("intercepted_url" to url),
                        sdkVersion = PaymentConstants.SDK_VERSION
                    )
                }
                
                // Handle callback URL if needed
                handleCallbackURL(url, orderID, orderToken,subMerchantId)
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                if (is_debug_enabled) {
                    Log.d(TAG, "Page finished loading: ${url ?: "empty"}")
                }
                super.onPageFinished(view, url)
                
                // Log page loaded event
                EventLoggingUtils.safeLogEvent(
                    context,
                    eventName = EventConstants.WEBVIEW_LOADED,
                    orderId = orderID,
                    orderToken,
                    subMerchantId,
                    additionalData = mapOf("loaded_url" to (url ?: "unknown")),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )
                
                // Send UPI intents once after page loads
                if (!isInitialPageLoaded) {
                    sendUpiIntents(orderID, orderToken, subMerchantId)
                    isInitialPageLoaded = true
                }
                
                // Notify callback that page has finished loading (only once)
                if (!hasCallbackBeenInvoked) {
                    onPageFinishedCallback?.invoke()
                    hasCallbackBeenInvoked = true
                }
            }
        }
    }
    
    /**
     * Handle callback URL processing
     */
    private fun handleCallbackURL(url: String?, orderID: String, orderToken: String,subMerchantId: String) {
        if (url != null && url.startsWith(RestApiUtils.WEB_VIEW_RESP_CHECK_URL)) {
            if (is_debug_enabled) {
                Log.d(TAG, "Processing callback URL: $url")
            }
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.URL_INTERCEPTED,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf("intercepted_url" to url),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
            
            // Create payment handler for callback processing
            val paymentHandler = PaymentHandler(context)
            paymentHandler.processCallbackURL(url, orderID, orderToken, subMerchantId)
        }
    }
    
    /**
     * Send UPI intents to WebView
     */
    private fun sendUpiIntents(orderID: String, orderToken: String, subMerchantId: String) {
        if (is_debug_enabled) {
            Log.d(TAG, "Sending UPI intents to WebView")
        }
        
        val upiManager = UPIManager(context)
        val upiAppsJSON = upiManager.createUPIAppsJSON()

        // Log UPI apps listed event
        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.UPI_APPS_LISTED,
            orderId = orderID,
            orderToken,
            subMerchantId = subMerchantId,
            additionalData = mapOf<String, Any>(
                "apps_list" to upiAppsJSON.toString()
            ),
            sdkVersion = PaymentConstants.SDK_VERSION
        )
        
        evaluateJavaScript(
            "window.nimbbl_web.UPIIntentAvailable(JSON.stringify(${upiAppsJSON}))"
        )
        
        if (is_debug_enabled) {
            Log.d(TAG, "UPI intents sent successfully")
        }
    }

    
    /**
     * Setup WebChrome client for handling new windows
     */
    private fun setupWebChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val newWebView = WebView(view?.context ?: context)
                // Configure new WebView settings to ensure popup payment experience work
                newWebView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    // Allow redirects
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                val transport = resultMsg?.obj as? WebViewTransport
                if (transport != null) {
                    transport.webView = newWebView
                    resultMsg.sendToTarget()
                } else {
                    Log.e(TAG, "WebViewTransport is null")
                }
                
                val dialog = Dialog(view?.context ?: context).apply {
                    setContentView(newWebView)
                    show()
                    window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dialog.dismiss()
                    }
                }
                
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (is_debug_enabled) {
                            Log.d(TAG, "New window page finished: ${url ?: "empty"}")
                        }
                        super.onPageFinished(view, url)
                    }
                }
                
                return true
            }
        }
    }
    
    // JavaScript interface should be added directly in the activity
    // to ensure proper @JavascriptInterface annotations
    
    /**
     * Evaluate JavaScript in WebView
     * @param script JavaScript code to execute
     */
    fun evaluateJavaScript(script: String) {
        if (isDestroyed()) {
            Log.w(TAG, "WebView is destroyed, cannot evaluate JavaScript")
            return
        }
        try {
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating JavaScript: ${e.message}", e)
        }
    }
    
    /**
     * Get current URL of WebView
     * @return Current URL or null
     */
    fun getCurrentUrl(): String? {
        return webView.url
    }
    
    /**
     * Load URL in WebView
     * @param url URL to load
     * @param orderID Order ID for logging (optional)
     * @param orderToken Order token for logging (optional)
     * @param subMerchantId Sub merchant ID for logging (optional)
     */
    fun loadUrl(url: String, orderID: String? = null, orderToken: String? = null, subMerchantId: String? = null) {
        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error loading URL in WebView: ${e.message}", e)
            }
            // Log webview failed event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.WEBVIEW_FAILED,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    "error_code" to "URL_LOAD_ERROR",
                    "error_description" to (e.message ?: "Unknown error"),
                    "url" to url
                ),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
        }
    }
    
    /**
     * Destroy WebView and clean up resources
     */
    fun destroy() {
        if (isDestroyed) {
            return
        }
        try {
            // Remove JavaScript interface
            webView.removeJavascriptInterface(PaymentConstants.JAVASCRIPT_INTERFACE_NAME)
            // Clear WebView clients with dummy implementations
            webView.webViewClient = object : WebViewClient() {
                // Dummy implementation to prevent null assignment
            }
            webView.webChromeClient = object : WebChromeClient() {
                // Dummy implementation to prevent null assignment
            }
            // Enhanced memory cleanup
            webView.settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = false
                setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE)
            }
            
            // Clear all caches and data
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            webView.clearMatches()
            
            // Force garbage collection of WebView resources
            System.gc()
            
            // Destroy WebView
            webView.destroy()
            isDestroyed = true
            if (is_debug_enabled) {
                Log.d(TAG, "WebView destroyed successfully with enhanced cleanup")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying WebView: ${e.message}", e)
        }
    }

    /**
     * Check if WebView has been destroyed
     * @return true if WebView has been destroyed
     */
    fun isDestroyed(): Boolean {
        return isDestroyed
    }

    /**
     * Handle network errors
     */
    private fun handleNetworkError() {
        // Try to reload the page after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (!isDestroyed()) {
                    webView.reload()
                    if (is_debug_enabled) {
                        Log.d(TAG, "Attempting to reload page after network error")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading page: ${e.message}", e)
            }
        }, 3000) // 3 second delay
    }
    
    /**
     * Handle timeout errors
     */
    private fun handleTimeoutError() {
        // Try to reload the page after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (!isDestroyed()) {
                    webView.reload()
                    if (is_debug_enabled) {
                        Log.d(TAG, "Attempting to reload page after timeout error")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading page: ${e.message}", e)
            }
        }, 5000) // 5 second delay
    }
    
    /**
     * Handle connection errors
     */
    private fun handleConnectionError() {
        // Show user-friendly error message
        Log.e(TAG, "Connection failed. Please check your internet connection.")
    }
    
    /**
     * Handle SSL errors
     */
    private fun handleSSLError() {
        // Log SSL error for debugging
        Log.e(TAG, "SSL handshake failed. This might be due to certificate issues.")
    }
    
    /**
     * Handle generic errors
     */
    private fun handleGenericError(errorCode: Int, errorDescription: String) {
        // Log generic error for debugging
        Log.e(TAG, "Generic WebView error occurred: $errorDescription (Code: $errorCode)")
    }
} 