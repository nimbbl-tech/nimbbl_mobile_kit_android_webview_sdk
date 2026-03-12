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
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import tech.nimbbl.coreapisdk.api.RestApiUtils
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_WEBVIEW
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.toString

/**
 * Manager class for handling WebView operations and configuration
 */
class WebViewManager(
    private val context: Context,
    private val webView: WebView
) {
    
    companion object {
        private val ERROR_SSL_HANDSHAKE = WebViewClient.ERROR_FAILED_SSL_HANDSHAKE
    }
    
    private var isInitialPageLoaded = false
    private var isWebViewLoadedEventLogged = false
    private var isDestroyed = false
    private var onPageFinishedCallback: (() -> Unit)? = null
    private var hasCallbackBeenInvoked = false

    private val callbackHandled = AtomicBoolean(false)
    
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
        callbackHandled.set(false)
        
        // Log webview launch event
        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.WEBVIEW_LAUNCH,
            orderId = orderID,
            orderToken,
            subMerchantId,
            additionalData = mapOf(EventConstants.KEY_URL to url),
            sdkVersion = PaymentConstants.SDK_VERSION
        )
        
        // Configure WebView settings first for better performance
        configureWebViewSettings()
        setupWebViewClient(orderID, orderToken,subMerchantId)
        setupWebChromeClient()
        
        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error loading URL: ${e.message}", e)
            // Log webview failed event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.WEBVIEW_FAILED,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    EventConstants.KEY_ERROR_CODE to "URL_LOAD_ERROR",
                    EventConstants.KEY_ERROR_DESCRIPTION to (e.message ?: "Unknown error"),
                    EventConstants.KEY_URL to url
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
                
                // Handle specific error types (WebViewClient constants)
                when (errorCode) {
                    WebViewClient.ERROR_HOST_LOOKUP -> {
                        SDKUtils.logError(TAG_WEBVIEW, "Network error: Host lookup failed for URL: $requestUrl")
                        handleNetworkError()
                    }
                    WebViewClient.ERROR_TIMEOUT -> {
                        SDKUtils.logError(TAG_WEBVIEW, "Network error: Timeout for URL: $requestUrl")
                        handleTimeoutError()
                    }
                    WebViewClient.ERROR_CONNECT -> {
                        SDKUtils.logError(TAG_WEBVIEW, "Network error: Connection failed for URL: $requestUrl")
                        handleConnectionError()
                    }
                    ERROR_SSL_HANDSHAKE -> {
                        SDKUtils.logError(TAG_WEBVIEW, "SSL error: Handshake failed for URL: $requestUrl")
                        handleSSLError()
                    }
                    else -> {
                        SDKUtils.logError(TAG_WEBVIEW, "WebView error: $errorDescription (Code: $errorCode) for URL: $requestUrl")
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
                        EventConstants.KEY_ERROR_CODE to errorCode.toString(),
                        EventConstants.KEY_ERROR_DESCRIPTION to errorDescription,
                        EventConstants.KEY_URL to requestUrl
                    ),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                SDKUtils.logDebug(TAG_WEBVIEW, "Page started loading: ${url ?: "empty"}")
            }
            
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                // Optimize resource loading for better performance
                val url = request?.url?.toString()

                if (url != null) {
                    SDKUtils.logDebug(TAG_WEBVIEW, "Intercepting request: $url")
                }
                
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val targetUrl = request?.url?.toString()
                val handled = handleCallbackURL(targetUrl, orderID, orderToken, subMerchantId)
                if (handled) {
                    SDKUtils.logDebug(TAG_WEBVIEW, "Callback URL consumed in shouldOverrideUrlLoading: $targetUrl")
                    return true
                }
                return false
            }
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                SDKUtils.logDebug(TAG_WEBVIEW, "URL history updated: ${url ?: "empty"} -> isReload: $isReload")

                // Log URL intercepted event
                if (url != null) {
                    EventLoggingUtils.safeLogEvent(
                        context,
                        eventName = EventConstants.REDIRECT_URL_HIT,
                        orderId = orderID,
                        orderToken,
                        subMerchantId,
                        additionalData = mapOf(EventConstants.KEY_INTERCEPTED_URL to url),
                        sdkVersion = PaymentConstants.SDK_VERSION
                    )
                }

                // Fallback interception path; safe due to atomic guard in handleCallbackURL
                handleCallbackURL(url, orderID, orderToken, subMerchantId)
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                SDKUtils.logDebug(TAG_WEBVIEW, "Page finished loading: ${url ?: "empty"}")
                super.onPageFinished(view, url)
                
                // Log page loaded event
                EventLoggingUtils.safeLogEvent(
                    context,
                    eventName = EventConstants.WEBVIEW_LOADED,
                    orderId = orderID,
                    orderToken,
                    subMerchantId,
                    additionalData = mapOf(EventConstants.KEY_LOADED_URL to (url ?: "unknown")),
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
    private fun handleCallbackURL(url: String?, orderID: String, orderToken: String,subMerchantId: String): Boolean {
        if (url.isNullOrEmpty()) return false
        if (!url.startsWith(RestApiUtils.WEB_VIEW_RESP_CHECK_URL)) return false

        // Atomic once-only processing across repeated WebView callbacks.
        if (!callbackHandled.compareAndSet(false, true)) {
            SDKUtils.logDebug(TAG_WEBVIEW, "Duplicate callback URL ignored: $url")
            return true
        }
        SDKUtils.logDebug(TAG_WEBVIEW, "Processing callback URL (first hit): $url")
        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.URL_INTERCEPTED,
            orderId = orderID,
            orderToken,
            subMerchantId,
            additionalData = mapOf(EventConstants.KEY_INTERCEPTED_URL to url),
            sdkVersion = PaymentConstants.SDK_VERSION
        )

        return try {
            val paymentHandler = PaymentHandler(context)
            paymentHandler.processCallbackURL(url, orderID, orderToken, subMerchantId)
            true
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error processing callback URL: ${e.message}", e)
            true
        }

    }
    
    /**
     * Send UPI intents to WebView
     */
    private fun sendUpiIntents(orderID: String, orderToken: String, subMerchantId: String) {
        SDKUtils.logDebug(TAG_WEBVIEW, "Sending UPI intents to WebView")

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
                EventConstants.KEY_APPS_LIST to upiAppsJSON.toString()
            ),
            sdkVersion = PaymentConstants.SDK_VERSION
        )
        
        evaluateJavaScript(
            "window.nimbbl_web.UPIIntentAvailable(JSON.stringify(${upiAppsJSON}))"
        )
        
        SDKUtils.logDebug(TAG_WEBVIEW, "UPI intents sent successfully")
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
                    setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                    // Additional preload optimizations
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false

                    // Memory optimizations
                    minimumFontSize = 8
                    minimumLogicalFontSize = 8
                    textZoom = 100

                    // Layout optimizations
                    layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                }
                val transport = resultMsg?.obj as? WebViewTransport
                if (transport != null) {
                    transport.webView = newWebView
                    resultMsg.sendToTarget()
                } else {
                    SDKUtils.logError(TAG_WEBVIEW, "WebViewTransport is null")
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
                        SDKUtils.logDebug(TAG_WEBVIEW, "New window page finished: ${url ?: "empty"}")
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
            SDKUtils.logWarning(TAG_WEBVIEW, "WebView is destroyed, cannot evaluate JavaScript")
            return
        }
        try {
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error evaluating JavaScript: ${e.message}", e)
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
            SDKUtils.logError(TAG_WEBVIEW, "Error loading URL in WebView: ${e.message}", e)
            // Log webview failed event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.WEBVIEW_FAILED,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    EventConstants.KEY_ERROR_CODE to "URL_LOAD_ERROR",
                    EventConstants.KEY_ERROR_DESCRIPTION to (e.message ?: "Unknown error"),
                    EventConstants.KEY_URL to url
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
            
            // Destroy WebView
            webView.destroy()
            isDestroyed = true
            SDKUtils.logDebug(TAG_WEBVIEW, "WebView destroyed successfully with enhanced cleanup")
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error destroying WebView: ${e.message}", e)
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
                    SDKUtils.logDebug(TAG_WEBVIEW, "Attempting to reload page after network error")
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_WEBVIEW, "Error reloading page: ${e.message}", e)
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
                    SDKUtils.logDebug(TAG_WEBVIEW, "Attempting to reload page after timeout error")
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_WEBVIEW, "Error reloading page: ${e.message}", e)
            }
        }, 5000) // 5 second delay
    }
    
    /**
     * Handle connection errors
     */
    private fun handleConnectionError() {
        // Show user-friendly error message
        SDKUtils.logError(TAG_WEBVIEW, "Connection failed. Please check your internet connection.")
    }
    
    /**
     * Handle SSL errors
     */
    private fun handleSSLError() {
        // Log SSL error for debugging
        SDKUtils.logError(TAG_WEBVIEW, "SSL handshake failed. This might be due to certificate issues.")
    }
    
    /**
     * Handle generic errors
     */
    private fun handleGenericError(errorCode: Int, errorDescription: String) {
        // Log generic error for debugging
        SDKUtils.logError(TAG_WEBVIEW, "Generic WebView error occurred: $errorDescription (Code: $errorCode)")
    }
} 