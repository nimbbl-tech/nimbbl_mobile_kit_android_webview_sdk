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
import android.webkit.ConsoleMessage
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import tech.nimbbl.coreapi.events.EventConstants
import org.json.JSONObject
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.core.CheckoutEnvironment
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_WEBVIEW
import java.util.concurrent.atomic.AtomicBoolean

class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val paymentHandler: PaymentHandler? = null
) {

    companion object {
        private val ERROR_SSL_HANDSHAKE = WebViewClient.ERROR_FAILED_SSL_HANDSHAKE
        private const val MAX_RETRIES = 2
        // setWebContentsDebuggingEnabled is a one-way static call; guard against redundant calls
        private val debuggingEnabled = AtomicBoolean(false)
    }

    private var _isDestroyed = false
    private var isInitialPageLoaded = false
    private var onPageFinishedCallback: (() -> Unit)? = null
    private var hasCallbackBeenInvoked = false
    private val callbackHandled = AtomicBoolean(false)

    private var networkRetryCount = 0
    private var timeoutRetryCount = 0
    // Stored so retry runnables and popup dialogs can be cleaned up in destroy()
    private val retryHandler = Handler(Looper.getMainLooper())
    private var popupDialog: Dialog? = null
    private var popupWebView: WebView? = null

    fun setOnPageFinishedCallback(callback: () -> Unit) {
        onPageFinishedCallback = callback
    }

    fun setupWebView(url: String, orderID: String, orderToken: String, subMerchantId: String) {
        isInitialPageLoaded = false
        hasCallbackBeenInvoked = false
        callbackHandled.set(false)
        networkRetryCount = 0
        timeoutRetryCount = 0

        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.WEBVIEW_LAUNCH,
            orderId = orderID,
            orderToken,
            subMerchantId,
            additionalData = mapOf(EventConstants.KEY_URL to url),
            sdkVersion = PaymentConstants.SDK_VERSION
        )

        configureWebViewSettings()
        setupWebViewClient(orderID, orderToken, subMerchantId)
        setupWebChromeClient()

        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error loading URL: ${e.message}", e)
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

    private fun configureWebViewSettings() {
        try {
            if (SDKUtils.isDebugLoggingEnabled() && debuggingEnabled.compareAndSet(false, true)) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        } catch (_: Exception) {}

        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT)

            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false

            blockNetworkImage = false
            blockNetworkLoads = false

            mediaPlaybackRequiresUserGesture = false

            minimumFontSize = 8
            minimumLogicalFontSize = 8
            textZoom = 100

            layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }

        applySecuritySettings(webView.settings)
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    }

    // Extracted so popup WebViews get identical security hardening
    private fun applySecuritySettings(settings: android.webkit.WebSettings) {
        settings.apply {
            allowFileAccess = false
            allowContentAccess = false
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
            setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            setGeolocationEnabled(false)
            databaseEnabled = false
        }
    }

    private var currentOrderID = ""
    private var currentOrderToken = ""
    private var currentSubMerchantId = ""

    private fun setupWebViewClient(orderID: String, orderToken: String, subMerchantId: String) {
        currentOrderID = orderID
        currentOrderToken = orderToken
        currentSubMerchantId = subMerchantId
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                // Sub-resource failures (images, scripts, etc.) must not trigger
                // payment failure events or retry logic
                if (request?.isForMainFrame != true) return

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
                val url = request?.url?.toString()
                if (url != null && SDKUtils.isDebugLoggingEnabled()) {
                    val method = request.method ?: "GET"
                    val headers = request.requestHeaders?.let { sanitizeHeaders(it) }.orEmpty()
                    SDKUtils.logDebug(TAG_WEBVIEW, "WebView request: $method $url headers=$headers")
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
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

                EventLoggingUtils.safeLogEvent(
                    context,
                    eventName = EventConstants.WEBVIEW_LOADED,
                    orderId = orderID,
                    orderToken,
                    subMerchantId,
                    additionalData = mapOf(EventConstants.KEY_LOADED_URL to (url ?: "unknown")),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )

                if (!isInitialPageLoaded) {
                    sendUpiIntents(orderID, orderToken, subMerchantId)
                    isInitialPageLoaded = true
                }

                if (!hasCallbackBeenInvoked) {
                    onPageFinishedCallback?.invoke()
                    onPageFinishedCallback = null  // release reference after single invocation
                    hasCallbackBeenInvoked = true
                }
            }
        }
    }

    private fun handleCallbackURL(url: String?, orderID: String, orderToken: String, subMerchantId: String): Boolean {
        if (url.isNullOrEmpty()) return false
        if (!url.startsWith(CheckoutEnvironment.webViewResponseUrl)) return false

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
            val handler = paymentHandler ?: PaymentHandler(context)
            val processed = handler.processCallbackURL(url, orderID, orderToken, subMerchantId)
            if (!processed) {
                handler.handlePaymentFailure(
                    orderID, orderToken, subMerchantId,
                    "Callback URL received without response parameter",
                    isSendEvent = true
                )
            }
            true
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error processing callback URL: ${e.message}", e)
            true
        }
    }

    private fun sendUpiIntents(orderID: String, orderToken: String, subMerchantId: String) {
        SDKUtils.logDebug(TAG_WEBVIEW, "Querying UPI apps (background)")

        UPIManager(context).createUPIAppsJSON { upiAppsJSON ->
            if (isDestroyed()) return@createUPIAppsJSON
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
            val jsonStr = JSONObject.quote(upiAppsJSON.toString())
            evaluateJavaScript("window.nimbbl_web.UPIIntentAvailable($jsonStr)")
            SDKUtils.logDebug(TAG_WEBVIEW, "UPI intents sent successfully")
        }
    }

    private fun setupWebChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (SDKUtils.isDebugLoggingEnabled() && consoleMessage != null) {
                    SDKUtils.logDebug(
                        TAG_WEBVIEW,
                        "Console: ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                    )
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val activityContext = view?.context ?: return false
                val newWebView = WebView(activityContext)
                newWebView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    minimumFontSize = 8
                    minimumLogicalFontSize = 8
                    textZoom = 100
                    layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                }
                applySecuritySettings(newWebView.settings)

                val transport = resultMsg?.obj as? WebViewTransport
                if (transport == null) {
                    SDKUtils.logError(TAG_WEBVIEW, "WebViewTransport is null")
                    newWebView.destroy()
                    return false
                }
                transport.webView = newWebView
                resultMsg.sendToTarget()

                val dialog = Dialog(activityContext).apply {
                    setContentView(newWebView)
                    show()
                    window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
                popupDialog = dialog
                popupWebView = newWebView

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dialog.dismiss()
                        popupDialog = null
                        window.destroy()
                        popupWebView = null
                    }
                }

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
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

    fun evaluateJavaScript(script: String) {
        if (isDestroyed()) {
            SDKUtils.logWarning(TAG_WEBVIEW, "WebView is destroyed, cannot evaluate JavaScript")
            return
        }
        // evaluateJavascript must run on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { evaluateJavaScript(script) }
            return
        }
        try {
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error evaluating JavaScript: ${e.message}", e)
        }
    }

    fun getCurrentUrl(): String? = webView.url

    fun loadUrl(url: String, orderID: String? = null, orderToken: String? = null, subMerchantId: String? = null) {
        try {
            webView.loadUrl(url)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error loading URL in WebView: ${e.message}", e)
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

    fun destroy() {
        if (_isDestroyed) return
        try {
            retryHandler.removeCallbacksAndMessages(null)
            popupDialog?.dismiss()
            popupDialog = null
            popupWebView?.destroy()
            popupWebView = null
            onPageFinishedCallback = null

            webView.removeJavascriptInterface(PaymentConstants.JAVASCRIPT_INTERFACE_NAME)
            webView.webViewClient = object : WebViewClient() {}
            webView.webChromeClient = object : WebChromeClient() {}
            webView.settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = false
                setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE)
            }
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            webView.clearMatches()
            webView.destroy()
            SDKUtils.logDebug(TAG_WEBVIEW, "WebView destroyed successfully with enhanced cleanup")
        } catch (e: Exception) {
            SDKUtils.logError(TAG_WEBVIEW, "Error destroying WebView: ${e.message}", e)
        } finally {
            _isDestroyed = true
        }
    }

    fun isDestroyed(): Boolean = _isDestroyed

    private fun handleNetworkError() {
        if (networkRetryCount >= MAX_RETRIES) {
            SDKUtils.logError(TAG_WEBVIEW, "Network error: max retries ($MAX_RETRIES) reached, giving up")
            return
        }
        networkRetryCount++
        retryHandler.postDelayed({
            try {
                if (!isDestroyed()) {
                    SDKUtils.logDebug(TAG_WEBVIEW, "Attempting reload after network error (attempt $networkRetryCount)")
                    webView.reload()
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_WEBVIEW, "Error reloading page: ${e.message}", e)
            }
        }, 3000L)
    }

    private fun handleTimeoutError() {
        if (timeoutRetryCount >= MAX_RETRIES) {
            SDKUtils.logError(TAG_WEBVIEW, "Timeout error: max retries ($MAX_RETRIES) reached, giving up")
            return
        }
        timeoutRetryCount++
        retryHandler.postDelayed({
            try {
                if (!isDestroyed()) {
                    SDKUtils.logDebug(TAG_WEBVIEW, "Attempting reload after timeout error (attempt $timeoutRetryCount)")
                    webView.reload()
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_WEBVIEW, "Error reloading page: ${e.message}", e)
            }
        }, 5000L)
    }

    private fun handleConnectionError() {
        SDKUtils.logError(TAG_WEBVIEW, "Connection failed. Please check your internet connection.")
        paymentHandler?.handlePaymentFailure(
            currentOrderID, currentOrderToken, currentSubMerchantId,
            "Connection failed. Please check your internet connection.",
            isSendEvent = true
        )
    }

    private fun handleSSLError() {
        SDKUtils.logError(TAG_WEBVIEW, "SSL handshake failed. This might be due to certificate issues.")
        paymentHandler?.handlePaymentFailure(
            currentOrderID, currentOrderToken, currentSubMerchantId,
            "Secure connection could not be established. Please try again.",
            isSendEvent = true
        )
    }

    private fun handleGenericError(errorCode: Int, errorDescription: String) {
        SDKUtils.logError(TAG_WEBVIEW, "Generic WebView error occurred: $errorDescription (Code: $errorCode)")
    }

    private fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        if (headers.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>(headers.size)
        for ((k, v) in headers) {
            if (k.equals("Authorization", ignoreCase = true) || k.equals("Cookie", ignoreCase = true)) {
                out[k] = "***"
            } else {
                out[k] = v
            }
        }
        return out
    }
}