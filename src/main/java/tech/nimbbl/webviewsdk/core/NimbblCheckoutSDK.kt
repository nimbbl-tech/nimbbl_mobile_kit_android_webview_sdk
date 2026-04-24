package tech.nimbbl.webviewsdk.core

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
import tech.nimbbl.coreapisdk.api.RestApiUtils
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.extensions.getDeviceFingerPrint
import tech.nimbbl.coreapisdk.utils.extensions.getDeviceID
import tech.nimbbl.coreapisdk.utils.extensions.getMerchantIDFromJwtToken
import tech.nimbbl.coreapisdk.utils.extensions.md5
import tech.nimbbl.coreapisdk.utils.extensions.parseJwtToken
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BROADCAST_PAYMENT_RESULT
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_INIT_FAILED
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_ORDER_ID
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_PAYMENT_DATA
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_PAYLOAD_FIELD
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION_KEY
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_CHECKOUT_SDK
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener
import tech.nimbbl.webviewsdk.utils.SDKUtils
import tech.nimbbl.webviewsdk.utils.ValidationUtils
import java.lang.ref.WeakReference


class NimbblCheckoutSDK private constructor() {
    private var myActivityReference: WeakReference<Activity>? = null
    private var receiver: PaymentEventBroadCastReceiver? = null
    @Volatile
    private var isReceiverRegistered = false
    // WeakReference prevents the singleton from leaking the Activity via the listener callback
    private var listener: WeakReference<NimbblCheckoutPaymentListener>? = null
    // applicationContext is safe to hold in a singleton; never store Activity context here
    private var mContext: Context? = null
    // Weak ref to the host Activity used only for startActivity so checkout opens in the same task
    private var hostActivity: WeakReference<Activity>? = null
    private var currentAppCode: String? = null

    /**
     * Enables SDK debug logging even for signed/release builds.
     *
     * Must be called before [init] — changes after init() are not guaranteed to
     * take effect for all in-flight operations.
     *
     * Pass `true` from the host app only when you explicitly want verbose logs.
     * Pass `null` to fall back to the SDK's default behavior (BuildConfig.DEBUG).
     */
    fun setDebugLoggingEnabled(enabled: Boolean?) {
        SDKUtils.setDebugLoggingEnabled(enabled)
    }

    fun checkout(options: NimbblCheckoutOptions?) {
        // Validate all prerequisites
        val validationResult = ValidationUtils.validateCheckoutPrerequisites(mContext, listener?.get(), options)
        if (validationResult is ValidationUtils.ValidationResult.Error) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_CHECKOUT_VALIDATION_FAILED.format(validationResult.message))
            SDKUtils.logEventWithVersion(
                mContext,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_CHECKOUT_VALIDATION_FAILED,
                    PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                    PaymentConstants.EVENT_KEY_ERROR_MESSAGE to validationResult.message,
                    PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                )
            )
            listener?.get()?.onCheckoutResponse(data = SDKUtils.createErrorResponse(validationResult.errorType))
            return
        }

        // Validation passed: mContext is guaranteed non-null from here on
        val ctx = mContext!!

        // CHECKOUT_005 & CHECKOUT_006: Check for null orderId and subMerchantId in token
        try {
            val safeOptions = options!!         // validated non-null above
            val token = safeOptions.orderToken!! // validated non-null above
            val orderId = parseJwtToken(ctx, token)
            val subMerchantId = getMerchantIDFromJwtToken(ctx, token)

            val orderIdValidation = ValidationUtils.validateOrderId(orderId)
            if (orderIdValidation is ValidationUtils.ValidationResult.Error) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ORDER_ID_VALIDATION_FAILED.format(orderIdValidation.message))
                SDKUtils.logEventWithVersion(
                    ctx,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    token,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_ORDER_ID_VALIDATION_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to orderIdValidation.message,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                listener?.get()?.onCheckoutResponse(data = SDKUtils.createErrorResponse(orderIdValidation.errorType))
                return
            }

            val subMerchantIdValidation = ValidationUtils.validateSubMerchantId(subMerchantId)
            if (subMerchantIdValidation is ValidationUtils.ValidationResult.Error) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SUBMERCHANT_ID_VALIDATION_FAILED.format(subMerchantIdValidation.message))
                SDKUtils.logEventWithVersion(
                    ctx,
                    EventConstants.EXCEPTION_THROWN,
                    orderId,
                    token,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_SUBMERCHANT_ID_VALIDATION_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to subMerchantIdValidation.message,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                listener?.get()?.onCheckoutResponse(data = SDKUtils.createErrorResponse(subMerchantIdValidation.errorType))
                return
            }

            SDKUtils.logEventWithVersion(
                ctx,
                EventConstants.CHECKOUT_INITIATED,
                orderId,
                token,
                subMerchantId
            )

        } catch (e: Exception) {
            SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_EVENT_LOGGING_FAILED_CHECKOUT.format(e.message))
            listener?.get()?.onCheckoutResponse(data = SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR))
            return
        }

        try {
            val intent = Intent(ctx, tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity::class.java).apply {
                putExtra(PaymentConstants.EXTRA_TAG_OPTIONS, options)
            }
            // Prefer launching from the live host Activity to keep the checkout in the same task.
            // Fall back to applicationContext (requires NEW_TASK) if the Activity has been GC'd.
            val launchCtx: Context = hostActivity?.get()?.takeIf { !it.isFinishing } ?: run {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx
            }
            launchCtx.startActivity(intent)
            SDKUtils.logDebug(TAG_CHECKOUT_SDK, "Checkout Activity launched; receiver registered in init()")
        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, "Checkout error: ${e.message}", e)
            SDKUtils.logEventWithVersion(
                ctx,
                EventConstants.EXCEPTION_THROWN,
                null,
                options?.orderToken,
                null,
                mapOf<String, Any>(
                    PaymentConstants.EVENT_KEY_ERROR to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                    PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                    PaymentConstants.EVENT_KEY_ERROR_MESSAGE to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                    PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                )
            )
            listener?.get()?.onCheckoutResponse(data = SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR))
        }
    }

    private fun dismissCheckoutActivity() {
        if (myActivityReference == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_DISMISS_ACTIVITY_REF_NULL)
            mContext?.let {
                SDKUtils.logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_MY_ACTIVITY_REF_NULL,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_DISMISS_ACTIVITY,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_MY_ACTIVITY_REF_NULL,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
            }
            return
        }

        myActivityReference?.get()?.let { activity ->
            Handler(Looper.getMainLooper()).post {
                try {
                    activity.finish()
                } catch (e: Exception) {
                    SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_FINISHING_ACTIVITY.format(e.message), e)
                }
            }
        } ?: SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_DISMISS_ACTIVITY_NULL)
        // Payment result callbacks are always delivered via handlePaymentResult, never here
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setMyActivityWeakReference(myActivityReference: Activity?) {
        if (myActivityReference == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SET_ACTIVITY_REF_NULL)
            if (mContext != null) {
                listener?.get()?.onCheckoutResponse(
                    data = SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR)
                )
            }
            return
        }

        this.myActivityReference = WeakReference(myActivityReference)
    }

    fun setEnvironmentUrl(apiUrl: String) {
        if (apiUrl.isEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ENV_URL_NULL)
            if (mContext != null) {
                listener?.get()?.onCheckoutResponse(
                    SDKUtils.createErrorResponse(ERROR_CODE_API_URL_NULL)
                )
            }
            return
        }

        val normalizedInputUrl = apiUrl.trim()
        val isIpUrl = isIpBasedUrl(normalizedInputUrl)

        // If caller passes an IP-based URL, we default API calls to QA3 (as per sample-app behavior),
        // but still allow launching WebView from the IP host (useful for local WebView testing).
        val resolvedApiBaseUrl = if (isIpUrl) {
            "https://qa3api.qa.nimbbl.tech/"
        } else {
            normalizedInputUrl
        }

        RestApiUtils.NIMBBL_TECH_URL = resolvedApiBaseUrl

        val webViewBaseUrl = if (isIpUrl) normalizedInputUrl else resolvedApiBaseUrl
        val webViewUrl = RestApiUtils.getWebViewUrl(webViewBaseUrl)
        val webViewRespUrl = RestApiUtils.getWebViewResponseUrl(webViewBaseUrl)

        RestApiUtils.WEB_VIEW_VIEW_URL = webViewUrl
        RestApiUtils.WEB_VIEW_RESP_CHECK_URL = webViewRespUrl

        SDKUtils.logDebug(
            TAG_CHECKOUT_SDK,
            PaymentConstants.LOG_MSG_ENV_SET.format(resolvedApiBaseUrl, webViewUrl, webViewRespUrl)
        )
    }

    private fun isIpBasedUrl(url: String): Boolean {
        return url.matches(Regex("https?://\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?/?.*"))
    }

    @JvmOverloads
    fun init(activity: Activity?, appCode: String? = null) {
        if (activity == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_INIT_ACTIVITY_NULL)
            return
        }

        val listenerImpl = activity as? NimbblCheckoutPaymentListener
            ?: throw IllegalArgumentException(
                "Activity must implement NimbblCheckoutPaymentListener. " +
                "Add `implements NimbblCheckoutPaymentListener` to your Activity."
            )
        listener = WeakReference(listenerImpl)
        // applicationContext never leaks; the Activity is held only weakly via hostActivity
        mContext = activity.applicationContext
        hostActivity = WeakReference(activity)
        synchronized(this) {
            if (receiver != null && isReceiverRegistered) {
                try { mContext!!.unregisterReceiver(receiver) } catch (_: Exception) {}
                isReceiverRegistered = false
            }
            receiver = PaymentEventBroadCastReceiver()
            // Register immediately so the receiver is ready before the Activity launches in checkout()
            try {
                ContextCompat.registerReceiver(
                    mContext!!, receiver, IntentFilter(BROADCAST_PAYMENT_RESULT),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                isReceiverRegistered = true
                SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_BROADCAST_REG_SUCCESS)
            } catch (e: Exception) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_REGISTERING_RECEIVER.format(e.message), e)
            }
        }

        if (!appCode.isNullOrEmpty()) {
            currentAppCode = appCode
        }

        val deviceFingerPrint = getDeviceFingerPrint()
        val deviceID = getDeviceID()

        if (deviceFingerPrint.isNullOrEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_INIT_FINGERPRINT_NULL)
            try {
                NimbblCoreApiSDK.getInstance()?.logEvent(
                    activity,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    subMerchantId = null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_DEVICE_FINGERPRINT_NULL,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_INIT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_DEVICE_FINGERPRINT_NULL,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    ),
                    null,
                    null,
                    mapOf("appVersion" to PaymentConstants.SDK_VERSION),
                )
            } catch (e: Exception) {
                SDKUtils.logWarning(
                    TAG_CHECKOUT_SDK,
                    PaymentConstants.LOG_MSG_EVENT_LOGGING_FAILED_FINGERPRINT.format(e.message)
                )
            }
            listener?.get()?.onCheckoutResponse(
                data = SDKUtils.createErrorResponse(ERROR_CODE_INIT_FAILED)
            )
            return
        }

        if (deviceID.isNullOrEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_INIT_DEVICE_ID_NULL)
            SDKUtils.logEventWithVersion(
                activity,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_DEVICE_ID_NULL,
                    PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_INIT_METHOD,
                    PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_DEVICE_ID_NULL,
                    PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                )
            )
            listener?.get()?.onCheckoutResponse(
                data = SDKUtils.createErrorResponse(ERROR_CODE_INIT_FAILED)
            )
            return
        }

        val fingerPrint = md5(deviceFingerPrint + deviceID)
        NimbblCoreApiSDK.getInstance()
            ?.initialiseAPISDK(RestApiUtils.NIMBBL_TECH_URL, fingerPrint, deviceFingerPrint, currentAppCode)

        SDKUtils.logEventWithVersion(
            activity,
            EventConstants.SDK_INITIALIZED,
            null,
            null,
            null,
            mapOf<String, Any>(SDK_VERSION_KEY to PaymentConstants.SDK_VERSION)
        )
    }

    private inner class PaymentEventBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (context == null) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RECEIVER_CONTEXT_NULL)
                return
            }

            val action = intent.action
            val data = intent.getStringExtra(EXTRA_PAYMENT_DATA)
            val orderId = intent.getStringExtra(EXTRA_ORDER_ID)

            if (action.isNullOrEmpty()) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RECEIVER_ACTION_NULL)
                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_ACTION_NULL,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_BROADCAST_RECEIVER,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_ACTION_NULL,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                return
            }

            if (action != BROADCAST_PAYMENT_RESULT) {
                SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RECEIVER_UNKNOWN_ACTION.format(action))
                return
            }

            // dismissCheckoutActivity handles null refs internally; the result callback is
            // delivered solely by handlePaymentResult, preventing any double-callback
            dismissCheckoutActivity()
            handlePaymentResult(context, data, orderId)
        }

        private fun handlePaymentResult(context: Context, data: String?, orderId: String?) {
            try {
                if (data.isNullOrEmpty()) {
                    SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_PAYMENT_DATA_NULL)
                    SDKUtils.logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_PAYMENT_DATA_NULL,
                            PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_BROADCAST_RECEIVER,
                            PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_PAYMENT_DATA_NULL,
                            PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                        )
                    )
                    listener?.get()?.onCheckoutResponse(SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR))
                    return
                }

                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.PAYMENT_STATUS,
                    orderId,
                    null,
                    null,
                    mapOf<String, Any>(PaymentConstants.EVENT_KEY_PAYMENT_DATA to data)
                )

                val l = listener?.get()
                if (l == null) {
                    SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_LISTENER_NULL_PAYMENT)
                    SDKUtils.logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_LISTENER_NULL,
                            PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_BROADCAST_RECEIVER,
                            PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_LISTENER_NULL,
                            PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                        )
                    )
                } else {
                    try {
                        val parsedData = jsonString2Map(data)
                        l.onCheckoutResponse(parsedData)
                    } catch (e: Exception) {
                        SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_PARSING_PAYMENT.format(e.message), e)
                        SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RAW_DATA.format(data.take(200)))
                        SDKUtils.logEventWithVersion(
                            context,
                            EventConstants.EXCEPTION_THROWN,
                            null,
                            null,
                            null,
                            mapOf<String, Any>(
                                PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_JSON_PARSING_FAILED,
                                PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_PAYMENT_SUCCESS,
                                PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_JSON_PARSING_FAILED,
                                PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                            )
                        )
                        // Return an explicit error so merchants can distinguish failure from success
                        l.onCheckoutResponse(SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR))
                    }
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, "Error handling payment result: ${e.message}", e)
                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_PAYMENT_SUCCESS,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                    )
                )
            }
        }
    }

    private fun jsonString2Map(jsonString: String): MutableMap<String, Any> {
        val keys: MutableMap<String, Any> = HashMap()

        if (jsonString.isBlank()) {
            SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_JSON_STRING_EMPTY)
            return keys
        }

        try {
            val jsonObject = JSONObject(jsonString)

            if (jsonObject.has(JSON_PAYLOAD_FIELD)) {
                val payloadString = jsonObject.optString(JSON_PAYLOAD_FIELD, "")
                if (payloadString.isNotEmpty()) {
                    try {
                        val payloadJson = JSONObject(payloadString)
                        val keyset: Iterator<*> = payloadJson.keys()
                        while (keyset.hasNext()) {
                            val key = keyset.next() as String
                            keys[key] = payloadJson[key]
                        }
                        return keys
                    } catch (e: JSONException) {
                        SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_PARSING_PAYLOAD.format(e.message), e)
                    }
                }
            }

            val keyset: Iterator<*> = jsonObject.keys()
            while (keyset.hasNext()) {
                val key = keyset.next() as String
                keys[key] = jsonObject[key]
            }
        } catch (e: JSONException) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_JSON_PARSING_ERROR.format(e.message), e)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_UNEXPECTED_ERROR.format(e.message), e)
        }

        return keys
    }

    // Must only be called while holding the `this` lock — reads isReceiverRegistered without locking itself
    private fun isCleanedUp(): Boolean {
        return mContext == null && listener == null && receiver == null &&
               myActivityReference == null && hostActivity == null && !isReceiverRegistered
    }

    fun cleanup() {
        var savedCtx: Context? = null
        try {
            synchronized(this) {
                if (isCleanedUp()) {
                    SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SDK_ALREADY_CLEANED)
                    return
                }
                savedCtx = mContext  // capture before clearing for catch-block logging
                if (receiver != null && mContext != null && isReceiverRegistered) {
                    try {
                        mContext!!.unregisterReceiver(receiver)
                        isReceiverRegistered = false
                        SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_BROADCAST_UNREG_SUCCESS)
                    } catch (e: Exception) {
                        SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_UNREG_RECEIVER.format(e.message), e)
                        isReceiverRegistered = false
                    }
                }
                hostActivity?.clear()
                hostActivity = null
                myActivityReference?.clear()
                myActivityReference = null
                listener = null
                mContext = null
                receiver = null
            }

            SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_CLEANUP_SUCCESS)

        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_DURING_CLEANUP.format(e.message), e)
            savedCtx?.let {
                SDKUtils.logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_SDK_CLEANUP_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CLEANUP_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_CLEANUP_ERROR),
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                    )
                )
            }
        }
    }

    companion object {
        // INSTANCE holds applicationContext (not Activity context), so no real leak exists.
        // @SuppressLint retained because lint cannot distinguish applicationContext from Activity context.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: NimbblCheckoutSDK? = null

        /**
         * Get singleton instance of NimbblCheckoutSDK.
         * Thread-safe implementation using double-checked locking.
         */
        @JvmStatic
        fun getInstance(): NimbblCheckoutSDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NimbblCheckoutSDK().also { INSTANCE = it }
            }
        }
    }
}
