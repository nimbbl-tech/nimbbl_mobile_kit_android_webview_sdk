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
import kotlin.collections.mapOf
import kotlin.text.startsWith
import kotlin.apply

/**
 * Manager class for handling WebView operations and configuration
 */
class WebViewManager(
    private val context: Context,
    private val webView: WebView
) {
    
    companion object {
        private const val TAG = "WebViewManager"
        private const val ERROR_HOST_LOOKUP = 101
        private const val ERROR_TIMEOUT = 102
        private const val ERROR_CONNECT = 103
        private const val ERROR_SSL_HANDSHAKE = 104
    }
    
    private var isInitialPageLoaded = false
    private var isWebViewLoadedEventLogged = false
    private var isDestroyed = false
    
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
        
        // Log webview launch event
        EventLoggingUtils.safeLogEvent(
            context,
            EventConstants.WEBVIEW_LAUNCH,
            orderID,
            orderToken,
            subMerchantId,
            mapOf("url" to url),
            PaymentConstants.SDK_VERSION
        )
        
        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error loading URL: ${e.message}", e)
            }
            // Log webview failed event
            EventLoggingUtils.safeLogEvent(
                context,
                EventConstants.WEBVIEW_FAILED,
                orderID,
                orderToken,
                subMerchantId,
                mapOf<String, Any>(
                    "error_code" to "URL_LOAD_ERROR",
                    "error_description" to (e.message ?: "Unknown error"),
                    "url" to url
                ),
                PaymentConstants.SDK_VERSION
            )
        }
        configureWebViewSettings()
        setupWebViewClient(orderID, orderToken,subMerchantId)
        setupWebChromeClient()
    }
    
    /**
     * Configure WebView settings
     */
    private fun configureWebViewSettings() {
        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }
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
                        handleNetworkError(orderID, orderToken, subMerchantId, requestUrl)
                    }
                    ERROR_TIMEOUT -> {
                        Log.e(TAG, "Network error: Timeout for URL: $requestUrl")
                        handleTimeoutError(orderID, orderToken, subMerchantId, requestUrl)
                    }
                    ERROR_CONNECT -> {
                        Log.e(TAG, "Network error: Connection failed for URL: $requestUrl")
                        handleConnectionError(orderID, orderToken, subMerchantId, requestUrl)
                    }
                    ERROR_SSL_HANDSHAKE -> {
                        Log.e(TAG, "SSL error: Handshake failed for URL: $requestUrl")
                        handleSSLError(orderID, orderToken, subMerchantId, requestUrl)
                    }
                    else -> {
                        Log.e(TAG, "WebView error: $errorDescription (Code: $errorCode) for URL: $requestUrl")
                        handleGenericError(orderID, orderToken, subMerchantId, requestUrl, errorCode, errorDescription)
                    }
                }
                
                // Log webview failed event
                EventLoggingUtils.safeLogEvent(
                    context,
                    EventConstants.WEBVIEW_FAILED,
                    orderID,
                    orderToken,
                    subMerchantId,
                    mapOf<String, Any>(
                        "error_code" to errorCode.toString(),
                        "error_description" to errorDescription,
                        "url" to requestUrl
                    ),
                    PaymentConstants.SDK_VERSION
                )
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
                        EventConstants.URL_INTERCEPTED,
                        orderID,
                        orderToken,
                        subMerchantId,
                        mapOf("intercepted_url" to url),
                        PaymentConstants.SDK_VERSION
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
                    EventConstants.WEBVIEW_LOADED,
                    orderID,
                    orderToken,
                    subMerchantId,
                    mapOf("loaded_url" to (url ?: "unknown")),
                    PaymentConstants.SDK_VERSION
                )
                
                // Send UPI intents once after page loads
                if (!isInitialPageLoaded) {
                    sendUpiIntents(orderID, orderToken, subMerchantId)
                    isInitialPageLoaded = true
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
        val upiAppsJSON = upiManager.createUPIAppsJSON(orderID, orderToken, subMerchantId)

        // Log UPI apps listed event
        EventLoggingUtils.safeLogEvent(
            context,
            "upi_apps_listed",
            orderID,
            orderToken,
            subMerchantId = subMerchantId,
            mapOf<String, Any>(
                "apps_list" to upiAppsJSON.toString()
            ),
            PaymentConstants.SDK_VERSION
        )
        
        evaluateJavaScript(
            "window.nimbbl_web.UPIIntentAvailable(JSON.stringify(${upiAppsJSON}))"
        )
        
        if (is_debug_enabled) {
            Log.d(TAG, "UPI intents sent successfully")
        }
    }
    
    /**
     * Reset the initial page loaded flag (useful for testing or page refreshes)
     */
    fun resetInitialPageLoaded() {
        isInitialPageLoaded = false
        if (is_debug_enabled) {
            Log.d(TAG, "Reset initial page loaded flag")
        }
    }
    
    /**
     * Reset the webview loaded event flag (useful for testing or new sessions)
     */
    fun resetWebViewLoadedEvent() {
        isWebViewLoadedEventLogged = false
        if (is_debug_enabled) {
            Log.d(TAG, "Reset webview loaded event flag")
        }
    }
    
    /**
     * Check if UPI intents have been sent
     * @return true if UPI intents have been sent
     */
    fun hasUpiIntentsBeenSent(): Boolean {
        return isInitialPageLoaded
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
                EventConstants.WEBVIEW_FAILED,
                orderID,
                orderToken,
                subMerchantId,
                mapOf<String, Any>(
                    "error_code" to "URL_LOAD_ERROR",
                    "error_description" to (e.message ?: "Unknown error"),
                    "url" to url
                ),
                PaymentConstants.SDK_VERSION
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
            // Clear settings
            webView.settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = false
            }
            // Clear cache and data
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            // Destroy WebView
            webView.destroy()
            isDestroyed = true
            if (is_debug_enabled) {
                Log.d(TAG, "WebView destroyed successfully")
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
    private fun handleNetworkError(orderID: String, orderToken: String, subMerchantId: String, url: String) {
        // Try to reload the page after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (!isDestroyed()) {
                    webView.reload()
                    Log.d(TAG, "Attempting to reload page after network error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading page: ${e.message}", e)
            }
        }, 3000) // 3 second delay
    }
    
    /**
     * Handle timeout errors
     */
    private fun handleTimeoutError(orderID: String, orderToken: String, subMerchantId: String, url: String) {
        // Try to reload the page after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (!isDestroyed()) {
                    webView.reload()
                    Log.d(TAG, "Attempting to reload page after timeout error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading page: ${e.message}", e)
            }
        }, 5000) // 5 second delay
    }
    
    /**
     * Handle connection errors
     */
    private fun handleConnectionError(orderID: String, orderToken: String, subMerchantId: String, url: String) {
        // Show user-friendly error message
        Log.e(TAG, "Connection failed. Please check your internet connection.")
    }
    
    /**
     * Handle SSL errors
     */
    private fun handleSSLError(orderID: String, orderToken: String, subMerchantId: String, url: String) {
        // Log SSL error for debugging
        Log.e(TAG, "SSL handshake failed. This might be due to certificate issues.")
    }
    
    /**
     * Handle generic errors
     */
    private fun handleGenericError(orderID: String, orderToken: String, subMerchantId: String, url: String, errorCode: Int, errorDescription: String) {
        // Log generic error for debugging
        Log.e(TAG, "Generic WebView error occurred: $errorDescription (Code: $errorCode)")
    }
} 