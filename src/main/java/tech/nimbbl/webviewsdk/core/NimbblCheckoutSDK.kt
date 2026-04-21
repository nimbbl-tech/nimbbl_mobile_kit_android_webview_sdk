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
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ACTION_PAYMENT_RESULT
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BROADCAST_PAYMENT_RESULT
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_TAG_OPTIONS
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
    private var myActivityReference: WeakReference<Activity?>? = null
    private var receiver: PaymentEventBroadCastReceiver? = null
    @Volatile
    private var isReceiverRegistered = false
    private var listener: NimbblCheckoutPaymentListener? = null
    private var mContext: Context? = null
    private var currentAppCode: String? = null

    /**
     * Enables SDK debug logging even for signed/release builds.
     *
     * Pass `true` from the host app only when you explicitly want verbose logs.
     * Pass `null` to fall back to the SDK's default behavior (BuildConfig.DEBUG).
     */
    fun setDebugLoggingEnabled(enabled: Boolean?) {
        SDKUtils.setDebugLoggingEnabled(enabled)
    }

    fun checkout(options: NimbblCheckoutOptions?) {
        // Validate all prerequisites
        val validationResult = ValidationUtils.validateCheckoutPrerequisites(mContext, listener, options)
        if (validationResult is ValidationUtils.ValidationResult.Error) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_CHECKOUT_VALIDATION_FAILED.format(validationResult.message))

            // Log validation failure event
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
            
            // Call listener with error if available
            listener?.onCheckoutResponse(data = SDKUtils.createErrorResponse(validationResult.errorType))
            return
        }

        // Store the appCode for later use

        // CHECKOUT_005 & CHECKOUT_006: Check for null orderId and subMerchantId in token
        try {
            val orderId = parseJwtToken(mContext!!, options?.orderToken!!)
            val subMerchantId = getMerchantIDFromJwtToken(mContext!!, options.orderToken!!)

            // Validate order ID
            val orderIdValidation = ValidationUtils.validateOrderId(orderId)
            if (orderIdValidation is ValidationUtils.ValidationResult.Error) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ORDER_ID_VALIDATION_FAILED.format(orderIdValidation.message))
                // Log validation failure event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_ORDER_ID_VALIDATION_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to orderIdValidation.message,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                // Call listener with checkout failed
                listener?.onCheckoutResponse(data = SDKUtils.createErrorResponse(orderIdValidation.errorType))
                return
            }

            // Validate sub merchant ID
            val subMerchantIdValidation = ValidationUtils.validateSubMerchantId(subMerchantId)
            if (subMerchantIdValidation is ValidationUtils.ValidationResult.Error) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SUBMERCHANT_ID_VALIDATION_FAILED.format(subMerchantIdValidation.message))
                // Log validation failure event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    orderId,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_SUBMERCHANT_ID_VALIDATION_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to subMerchantIdValidation.message,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                // Call listener with checkout failed
                listener?.onCheckoutResponse(data = SDKUtils.createErrorResponse(subMerchantIdValidation.errorType))
                return
            }

            // Log checkout initiated event (non-blocking)
            SDKUtils.logEventWithVersion(
                mContext!!,
                EventConstants.CHECKOUT_INITIATED,
                orderId,
                options.orderToken,
                subMerchantId
            )

        } catch (e: Exception) {
            // Event logging failed but don't impact the main flow
            SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_EVENT_LOGGING_FAILED_CHECKOUT.format(e.message))
        }

        try {

            val intent = Intent(
                mContext,
                tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity::class.java
            )
            intent.putExtra(PaymentConstants.EXTRA_TAG_OPTIONS, options)
            mContext!!.startActivity(intent)

            // Check for null receiver before registering
            if (receiver == null) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RECEIVER_NULL)
                // Log null receiver event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options!!.orderToken,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_RECEIVER_NULL,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_RECEIVER_NULL,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace()
                    )
                )
                return
            }

            // Use ContextCompat to handle flags automatically for all API levels
            try {
                // Check if receiver is already registered to prevent double registration
                // Synchronize to prevent race conditions
                synchronized(this) {
                    when {
                        mContext != null && !isReceiverRegistered -> {
                            ContextCompat.registerReceiver(
                                mContext!!, receiver, IntentFilter(BROADCAST_PAYMENT_RESULT),
                                ContextCompat.RECEIVER_NOT_EXPORTED
                            )
                            isReceiverRegistered = true
                            SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_BROADCAST_REG_SUCCESS)
                        }
                        isReceiverRegistered -> {
                            SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_BROADCAST_ALREADY_REGISTERED)
                        }
                        else -> {
                            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_CONTEXT_NULL_REGISTER)
                        }
                    }
                }
            } catch (e: Exception) {
               SDKUtils.logError(
                   TAG_CHECKOUT_SDK,
                   PaymentConstants.LOG_MSG_ERROR_REGISTERING_RECEIVER.format(e.message),
                   e
               )
                // Log exception event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options!!.orderToken,
                    null,
                    mapOf<String, Any>(
                        PaymentConstants.EVENT_KEY_ERROR to PaymentConstants.EVENT_ERROR_BROADCAST_REGISTRATION_FAILED,
                        PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                        PaymentConstants.EVENT_KEY_ERROR_MESSAGE to PaymentConstants.EVENT_MSG_BROADCAST_REG_FAILED,
                        PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                    )
                )
            }
        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, "Checkout error: ${e.message}", e)
            // Log exception thrown event (non-blocking)
            SDKUtils.logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null, // orderId will be extracted from token in MainActivity
                options!!.orderToken,
                null,
                mapOf<String, Any>(
                    PaymentConstants.EVENT_KEY_ERROR to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                    PaymentConstants.EVENT_KEY_LOCATION to PaymentConstants.EVENT_LOCATION_CHECKOUT_METHOD,
                    PaymentConstants.EVENT_KEY_ERROR_MESSAGE to (e.message ?: PaymentConstants.EVENT_MSG_UNKNOWN_ERROR),
                    PaymentConstants.EVENT_KEY_STACK_TRACE to SDKUtils.getStackTrace(e)
                )
            )
        }
    }

    fun dismissReactActivity() {
        // Check for null myActivityReference
        if (myActivityReference == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_DISMISS_ACTIVITY_REF_NULL)
            // Log null myActivityReference event
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
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                try {
                    activity.finish()
                } catch (e: Exception) {
                    SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_FINISHING_ACTIVITY.format(e.message), e)
                }
            }
        } ?: run {
            SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_DISMISS_ACTIVITY_NULL)
            // Call listener with payment failed if context is available, otherwise just log
            if (mContext != null) {
                listener?.onCheckoutResponse(
                    data = SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR)
                )
            }
        }
    }

    fun setMyActivityWeakReference(myActivityReference: Activity?) {
        // Check for null activity
        if (myActivityReference == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SET_ACTIVITY_REF_NULL)
            // Call listener with payment failed if context is available, otherwise just log
            if (mContext != null) {
                listener?.onCheckoutResponse(
                    data = SDKUtils.createErrorResponse(ERROR_CODE_UNKNOWN_ERROR)
                )
            }
            return
        }

        this.myActivityReference = WeakReference(myActivityReference)
    }

    fun setEnvironmentUrl(apiUrl: String) {
        // Check for null or empty apiUrl
        if (apiUrl.isEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ENV_URL_NULL)
            // Call listener with payment failed if context is available, otherwise just log
            if (mContext != null) {
            listener?.onCheckoutResponse(
                SDKUtils.createErrorResponse(ERROR_CODE_API_URL_NULL)
            )
            }
            return
        }

        val normalizedInputUrl = apiUrl.trim()

        // If caller passes an IP-based URL, we default API calls to QA3 (as per sample-app behavior),
        // but still allow launching WebView from the IP host (useful for local WebView testing).
        val resolvedApiBaseUrl = if (isIpBasedUrl(normalizedInputUrl)) {
            "https://qa3api.qa.nimbbl.tech/"
        } else {
            normalizedInputUrl
        }

        // Set base API URL (used by Core API SDK)
        RestApiUtils.NIMBBL_TECH_URL = resolvedApiBaseUrl

        // Generate WebView URLs. For IP input, keep the IP for WebView launch.
        val webViewBaseUrl = if (isIpBasedUrl(normalizedInputUrl)) normalizedInputUrl else resolvedApiBaseUrl
        val webViewUrl = RestApiUtils.getWebViewUrl(webViewBaseUrl)
        val webViewRespUrl = RestApiUtils.getWebViewResponseUrl(webViewBaseUrl)

        // Update the RestApiUtils URLs
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
            // Cannot call listener since activity is null and listener is set to activity
            // Just return without calling listener
            return
        }

        listener = activity as? NimbblCheckoutPaymentListener
        if (listener == null) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, "Activity must implement NimbblCheckoutPaymentListener")
            return
        }
        mContext = activity
        receiver = PaymentEventBroadCastReceiver()
        isReceiverRegistered = false
        
        // Set the app code if provided
        if (!appCode.isNullOrEmpty()) {
            currentAppCode = appCode
        }

        // Check for null device fingerprint or device ID
        val deviceFingerPrint = getDeviceFingerPrint()
        val deviceID = getDeviceID()

        if (deviceFingerPrint.isNullOrEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_INIT_FINGERPRINT_NULL)
            // Log null deviceFingerPrint event
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
                    null, // customUserAgent
                    null, // customDeviceInfo
                    mapOf("appVersion" to PaymentConstants.SDK_VERSION), // customAppInfo
                )
            } catch (e: Exception) {
                SDKUtils.logWarning(
                    TAG_CHECKOUT_SDK,
                    PaymentConstants.LOG_MSG_EVENT_LOGGING_FAILED_FINGERPRINT.format(e.message)
                )
            }
            // Return initialization error
            listener?.onCheckoutResponse(
                data = SDKUtils.createErrorResponse(ERROR_CODE_INIT_FAILED)
            )
            return
        }

        if (deviceID.isNullOrEmpty()) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_INIT_DEVICE_ID_NULL)
            // Log null deviceID event
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
            // Return initialization error
            listener?.onCheckoutResponse(
                data = SDKUtils.createErrorResponse(ERROR_CODE_INIT_FAILED)
            )
            return
        }

        val fingerPrint = md5(deviceFingerPrint + deviceID)
        NimbblCoreApiSDK.getInstance()
            ?.initialiseAPISDK(RestApiUtils.NIMBBL_TECH_URL, fingerPrint, deviceFingerPrint, currentAppCode)

        // Log SDK initialized event (non-blocking)
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
            // Check for null context
            if (context == null) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RECEIVER_CONTEXT_NULL)
                return
            }

            // Intent is guaranteed to be non-null as it's a parameter

            val action = intent.action
            val data = intent.getStringExtra(EXTRA_PAYMENT_DATA)
            val orderId = intent.getStringExtra(EXTRA_ORDER_ID)

            // Check for null action
            if (action.isNullOrEmpty()) {
                SDKUtils.logError(
                    TAG_CHECKOUT_SDK,
                    PaymentConstants.LOG_MSG_RECEIVER_ACTION_NULL
                )
                // Log null action event
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

            // Validate action is one of the expected actions
            if (action != ACTION_PAYMENT_RESULT) {
                SDKUtils.logWarning(
                    TAG_CHECKOUT_SDK,
                    PaymentConstants.LOG_MSG_RECEIVER_UNKNOWN_ACTION.format(action)
                )
                return
            }

            // Handle activity dismissal for both success and failure
            myActivityReference?.get()?.let { _ ->
                dismissReactActivity()
            } ?: run {
                SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ACTIVITY_REF_NULL)
            }

            handlePaymentResult(context, data, orderId)
        }

        /**
         * Handle payment success
         */
        private fun handlePaymentResult(context: Context, data: String?, orderId: String?) {
            try {
                // Check for null data
                if (data.isNullOrEmpty()) {
                    SDKUtils.logError(
                        TAG_CHECKOUT_SDK,
                        PaymentConstants.LOG_MSG_PAYMENT_DATA_NULL
                    )
                    // Log null data event
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
                    return
                }

                // Use orderId from broadcast intent

                // Log payment success event (non-blocking)
                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.PAYMENT_STATUS,
                    orderId,
                    null,
                    null,
                    mapOf<String, Any>(PaymentConstants.EVENT_KEY_PAYMENT_DATA to data)
                )

                // Check for null listener
                if (listener == null) {
                    SDKUtils.logError(
                        TAG_CHECKOUT_SDK,
                        PaymentConstants.LOG_MSG_LISTENER_NULL_PAYMENT
                    )
                    // Log null listener event
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
                        listener?.onCheckoutResponse(parsedData)
                    } catch (e: Exception) {
                        SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_PARSING_PAYMENT.format(e.message), e)
                        SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_RAW_DATA.format(data.take(200)))
                        // Log parsing error event (non-blocking)
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
                        // Return empty map as fallback
                        listener?.onCheckoutResponse(mutableMapOf())
                    }
                }
            } catch (e: Exception) {
                SDKUtils.logError(TAG_CHECKOUT_SDK, "Error handling payment result: ${e.message}", e)
                // Log payment failed event (non-blocking)
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
        
        // Validate input
        if (jsonString.isBlank()) {
            SDKUtils.logWarning(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_JSON_STRING_EMPTY)
            return keys
        }
        
        try {
            val jsonObject = JSONObject(jsonString)
            
            // Check if this has a payload field and extract only the payload content
            if (jsonObject.has(JSON_PAYLOAD_FIELD)) {
                val payloadString = jsonObject.optString(JSON_PAYLOAD_FIELD, "")
                if (payloadString.isNotEmpty()) {
                    try {
                        val payloadJson = JSONObject(payloadString)
                        val keyset: Iterator<*> = payloadJson.keys()
                        while (keyset.hasNext()) {
                            val key = keyset.next() as String
                            val value = payloadJson[key]
                            keys[key] = value
                        }
                        return keys
                    } catch (e: JSONException) {
                        SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_PARSING_PAYLOAD.format(e.message), e)
                    }
                }
            }
            
            // Fallback to original behavior if no payload field or parsing fails
            val keyset: Iterator<*> = jsonObject.keys()
            while (keyset.hasNext()) {
                val key = keyset.next() as String
                val value = jsonObject[key]
                keys[key] = value
            }
        } catch (e: JSONException) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_JSON_PARSING_ERROR.format(e.message), e)
        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_UNEXPECTED_ERROR.format(e.message), e)
        }
        
        return keys
    }


    /**
     * Check if SDK is cleaned up
     * @return true if SDK is cleaned up, false otherwise
     */
    fun isCleanedUp(): Boolean {
        return mContext == null && listener == null && receiver == null && myActivityReference == null && !isReceiverRegistered
    }

    /**
     * Clean up SDK resources
     * This method should be called when the SDK is no longer needed
     * to prevent memory leaks and properly unregister broadcast receivers
     */
    fun cleanup() {
        // Prevent multiple cleanup calls
        if (isCleanedUp()) {
            SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_SDK_ALREADY_CLEANED)
            return
        }

        try {
            // Store context reference for logging before clearing
            // Unregister broadcast receiver if registered
            // Synchronize to prevent race conditions during cleanup
            synchronized(this) {
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
            }

            // Clear activity reference
            myActivityReference?.clear()
            myActivityReference = null

            // Clear listener reference
            listener = null

            // Clear context reference
            mContext = null

            // Clear receiver reference
            receiver = null

            SDKUtils.logDebug(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_CLEANUP_SUCCESS)

        } catch (e: Exception) {
            SDKUtils.logError(TAG_CHECKOUT_SDK, PaymentConstants.LOG_MSG_ERROR_DURING_CLEANUP.format(e.message), e)

            // Log cleanup failure event using stored context reference
            mContext?.let {
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
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: NimbblCheckoutSDK? = null

        /**
         * Get singleton instance of NimbblCheckoutSDK
         * Thread-safe implementation using double-checked locking
         */
        @JvmStatic
        fun getInstance(): NimbblCheckoutSDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NimbblCheckoutSDK().also { INSTANCE = it }
            }
        }


        
    }
}