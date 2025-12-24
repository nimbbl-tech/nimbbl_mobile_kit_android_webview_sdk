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
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
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
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_PAYMENT_DATA
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener
import tech.nimbbl.webviewsdk.utils.RestApiUtils.NIMBBL_TECH_URL
import tech.nimbbl.webviewsdk.utils.RestApiUtils.WEB_VIEW_RESP_CHECK_URL
import tech.nimbbl.webviewsdk.utils.RestApiUtils.WEB_VIEW_VIEW_URL
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

    fun checkout(options: NimbblCheckoutOptions?) {
        // Validate all prerequisites
        val validationResult = ValidationUtils.validateCheckoutPrerequisites(mContext, listener, options)
        if (validationResult is ValidationUtils.ValidationResult.Error) {
            SDKUtils.logError("NimbblCheckoutSDK", "checkout validation failed: ${validationResult.message}")
            
            // Log validation failure event
            SDKUtils.logEventWithVersion(
                mContext,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "checkout_validation_failed",
                    "location" to "checkout_method",
                    "error_message" to validationResult.message,
                    "stack_trace" to SDKUtils.getStackTrace()
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
                SDKUtils.logError("NimbblCheckoutSDK", "checkout: orderId validation failed: ${orderIdValidation.message}")
                // Log validation failure event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "checkout_order_id_validation_failed",
                        "location" to "checkout_method",
                        "error_message" to orderIdValidation.message,
                        "stack_trace" to SDKUtils.getStackTrace()
                    )
                )
                // Call listener with checkout failed
                listener?.onCheckoutResponse(data = SDKUtils.createErrorResponse(orderIdValidation.errorType))
                return
            }

            // Validate sub merchant ID
            val subMerchantIdValidation = ValidationUtils.validateSubMerchantId(subMerchantId)
            if (subMerchantIdValidation is ValidationUtils.ValidationResult.Error) {
                SDKUtils.logError("NimbblCheckoutSDK", "checkout: subMerchantId validation failed: ${subMerchantIdValidation.message}")
                // Log validation failure event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    orderId,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "checkout_submerchant_id_validation_failed",
                        "location" to "checkout_method",
                        "error_message" to subMerchantIdValidation.message,
                        "stack_trace" to SDKUtils.getStackTrace()
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
            if (is_debug_enabled) {
                Log.w(
                    "NimbblCheckoutSDK",
                    "Event logging failed for checkout_initiated: ${e.message}"
                )
            }
        }

        try {

            val intent = Intent(
                mContext,
                tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity::class.java
            )
            intent.putExtra("options", options)
            mContext!!.startActivity(intent)

            // Check for null receiver before registering
            if (receiver == null) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "checkout: receiver is null")
                }
                // Log null receiver event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options!!.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "receiver_is_null",
                        "location" to "checkout_method",
                        "error_message" to "Receiver is null",
                        "stack_trace" to SDKUtils.getStackTrace()
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
                                ContextCompat.RECEIVER_EXPORTED
                            )
                            isReceiverRegistered = true
                            if (is_debug_enabled) {
                                Log.d("NimbblCheckoutSDK", "Broadcast receivers registered successfully")
                            } else {
                                // Explicit else for Kotlin compiler
                            }
                        }
                        isReceiverRegistered -> {
                            if (is_debug_enabled) {
                                Log.d("NimbblCheckoutSDK", "Broadcast receivers already registered")
                            } else {
                                // Explicit else for Kotlin compiler
                            }
                        }
                        else -> {
                            Log.e("NimbblCheckoutSDK", "Context is null, cannot register receivers")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NimbblCheckoutSDK", "Error registering broadcast receivers: ${e.message}", e)
                // Log exception event
                SDKUtils.logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options!!.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "broadcast_receiver_registration_failed",
                        "location" to "checkout_method",
                        "error_message" to "Failed to register broadcast receivers",
                        "stack_trace" to SDKUtils.getStackTrace(e)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Log exception thrown event (non-blocking)
            SDKUtils.logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null, // orderId will be extracted from token in MainActivity
                options!!.orderToken,
                null,
                mapOf<String, Any>(
                    "error" to (e.message ?: "Unknown error"),
                    "location" to "checkout_method",
                    "error_message" to (e.message ?: "Unknown error"),
                    "stack_trace" to SDKUtils.getStackTrace(e)
                )
            )
        }
    }

    fun dismissReactActivity() {
        // Check for null myActivityReference
        if (myActivityReference == null) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "dismissReactActivity: myActivityReference is null")
            }
            // Log null myActivityReference event
            mContext?.let {
                SDKUtils.logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "my_activity_reference_is_null",
                        "location" to "dismissReactActivity",
                        "error_message" to "MyActivityReference is null",
                        "stack_trace" to SDKUtils.getStackTrace()
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
                    Log.e("NimbblCheckoutSDK", "Error finishing activity: ${e.message}", e)
                }
            }
        } ?: run {
            if (is_debug_enabled) {
                Log.w("NimbblCheckoutSDK", "dismissReactActivity: activity is null")
            }
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
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "setMyActivityWeakReference: activity is null")
            }
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
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "setEnvironmentUrl: apiUrl is null or empty")
            }
            // Call listener with payment failed if context is available, otherwise just log
            if (mContext != null) {
            listener?.onCheckoutResponse(
                SDKUtils.createErrorResponse(ERROR_CODE_API_URL_NULL)
            )
            }
            return
        }

        NIMBBL_TECH_URL = apiUrl
        var webViewUrl: String
        var webViewRespUrl: String

        // Handle dynamic QA URLs and custom environments (matching iOS implementation)
        when {
            apiUrl.contains("qa1api.nimbbl.tech") -> {
                webViewUrl = "https://qa1sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://qa1sonic.nimbbl.tech/mobile/redirect"
            }

            apiUrl.contains("qa2api.nimbbl.tech") -> {
                 //webViewUrl = "http://192.168.1.17:4200/?token=%1\$s"
                 //webViewRespUrl = "http://192.168.1.17:4200/mobile/redirect"
                webViewUrl = "https://qa2sonic.nimbbl.tech/?token=%1\$s"
               webViewRespUrl = "https://qa2sonic.nimbbl.tech/mobile/redirect"
            }

            apiUrl.contains("qa4api.nimbbl.tech") -> {
               // webViewUrl = "http://192.168.1.19:4200/?token=%1\$s"
               // webViewRespUrl = "http://192.168.1.19:4200/mobile/redirect"
                webViewUrl = "https://qa4sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://qa4sonic.nimbbl.tech/mobile/redirect"
            }
            /*apiUrl.contains("qa3api.nimbbl.tech") -> {
                 webViewUrl = "http://192.168.0.30:4200/?token=%1\$s"
                 webViewRespUrl = "http://192.168.0.30:4200/mobile/redirect"
            }*/

            apiUrl.contains("qa") && apiUrl.contains("nimbbl.tech") -> {
                // Handle any other QA environment dynamically (matching iOS)
                val qaNumber = extractQaNumber(apiUrl)
                if (qaNumber != null) {
                    webViewUrl = "https://qa${qaNumber}sonic.nimbbl.tech/?token=%1\$s"
                    webViewRespUrl = "https://qa${qaNumber}sonic.nimbbl.tech/mobile/redirect"
                } else {
                    // Fallback for unknown QA environments
                    //  webViewUrl = "http://192.168.0.12:4200/?token=%1\$s"
                    //  webViewRespUrl = "http://192.168.0.12:4200/mobile/redirect"
                    webViewUrl = "https://qa4sonic.nimbbl.tech/?token=%1\$s"
                    webViewRespUrl = "https://qa4sonic.nimbbl.tech/mobile/redirect"
                }
            }

            apiUrl.contains("apipp.nimbbl.tech") -> {
                // Pre-production (matching iOS)

                //webViewUrl = "http://192.168.1.55:4200/?token=%1\$s"
                //webViewRespUrl = "http://192.168.1.55:4200/mobile/redirect"

                webViewUrl = "https://sonicpp.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://sonicpp.nimbbl.tech/mobile/redirect"
            }

            apiUrl.contains("api.nimbbl.tech") && !apiUrl.contains("qa") && !apiUrl.contains("pp") -> {
                // Production (matching iOS)
               // webViewUrl = "https://sonic-preview.nimbbl.tech/?token=%1\$s"
               // webViewRespUrl = "https://sonic-preview.nimbbl.tech/mobile/redirect"
                webViewUrl = "https://sonic.nimbbl.tech/?token=%1\$s"
               webViewRespUrl = "https://sonic.nimbbl.tech/mobile/redirect"
                //  webViewUrl = "http://192.168.0.30:4200/?token=%1\$s"
                // webViewRespUrl = "http://192.168.0.30:4200/mobile/redirect"
            }

            else -> {
                // Handle custom environments or unknown URLs (matching iOS fallback)
                if (is_debug_enabled) {
                    Log.w(
                        "NimbblCheckoutSDK",
                        "Unknown environment URL: $apiUrl, using production as fallback"
                    )
                }
               // webViewUrl = "https://sonic-preview.nimbbl.tech/?token=%1\$s"
               // webViewRespUrl = "https://sonic-preview.nimbbl.tech/mobile/redirect"
                webViewUrl = "https://sonic.nimbbl.tech/?token=%1\$s"
               webViewRespUrl = "https://sonic.nimbbl.tech/mobile/redirect"
            }
        }

        WEB_VIEW_VIEW_URL = webViewUrl
        WEB_VIEW_RESP_CHECK_URL = webViewRespUrl

        if (is_debug_enabled) {
            Log.d(
                "NimbblCheckoutSDK",
                "Environment set - API: $apiUrl, WebView: $webViewUrl, Resp: $webViewRespUrl"
            )
        }
    }

    /**
     * Extract QA number from URL (e.g., "qa4api.nimbbl.tech" -> "4")
     */
    private fun extractQaNumber(url: String): String? {
        // Check for null or empty url
        if (url.isNullOrEmpty()) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "extractQaNumber: url is null or empty")
            }
            return null
        }

        val regex = Regex("qa(\\d+)api\\.nimbbl\\.tech")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }

    // Added @JvmOverloads so Java callers can use init(activity) without passing appCode
    @JvmOverloads
    fun init(activity: Activity?, appCode: String? = null) {
        if (activity == null) {
            if (is_debug_enabled) {
                Log.e("Nimbbl SDK", "activity can't be null. returning.")
            }
            // Cannot call listener since activity is null and listener is set to activity
            // Just return without calling listener
            return
        }

        listener = activity as NimbblCheckoutPaymentListener?
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
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "init: deviceFingerPrint is null or empty")
            }
            // Log null deviceFingerPrint event
            try {
                NimbblCoreApiSDK.getInstance()?.logEvent(
                    activity,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    subMerchantId = null,
                    mapOf<String, Any>(
                        "error" to "device_fingerprint_is_null_or_empty",
                        "location" to "init_method",
                        "error_message" to "Device fingerprint is null or empty",
                        "stack_trace" to SDKUtils.getStackTrace()
                    ),
                    null, // customUserAgent
                    null, // customDeviceInfo
                    mapOf("appVersion" to PaymentConstants.SDK_VERSION), // customAppInfo
                )
            } catch (e: Exception) {
                if (is_debug_enabled) {
                    Log.w(
                        "NimbblCheckoutSDK",
                        "Event logging failed for null deviceFingerPrint: ${e.message}"
                    )
                }
            }
            // Return initialization error
            listener?.onCheckoutResponse(
                data = SDKUtils.createErrorResponse(ERROR_CODE_INIT_FAILED)
            )
            return
        }

        if (deviceID.isNullOrEmpty()) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "init: deviceID is null or empty")
            }
            // Log null deviceID event
            SDKUtils.logEventWithVersion(
                activity,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "device_id_is_null_or_empty",
                    "location" to "init_method",
                    "error_message" to "Device ID is null or empty",
                    "stack_trace" to SDKUtils.getStackTrace()
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
            ?.initialiseAPISDK(NIMBBL_TECH_URL, fingerPrint, deviceFingerPrint, currentAppCode)

        // Log SDK initialized event (non-blocking)
        SDKUtils.logEventWithVersion(
            activity,
            EventConstants.SDK_INITIALIZED,
            null,
            null,
            null,
            mapOf<String, Any>("sdk_version" to PaymentConstants.SDK_VERSION)
        )

    }

    private inner class PaymentEventBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Check for null context
            if (context == null) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: context is null")
                }
                return
            }

            // Intent is guaranteed to be non-null as it's a parameter

            val action = intent.action
            val data = intent.getStringExtra(EXTRA_PAYMENT_DATA)
            val orderId = intent.getStringExtra("order_id")

            // Check for null action
            if (action.isNullOrEmpty()) {
                if (is_debug_enabled) {
                    Log.e(
                        "NimbblCheckoutSDK",
                        "PaymentEventBroadCastReceiver: action is null or empty"
                    )
                }
                // Log null action event
                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "action_is_null_or_empty",
                        "location" to "PaymentEventBroadCastReceiver",
                        "error_message" to "Action is null or empty",
                        "stack_trace" to SDKUtils.getStackTrace()
                    )
                )
                return
            }

            // Validate action is one of the expected actions
            if (action != "PaymentResult") {
                if (is_debug_enabled) {
                    Log.w(
                        "NimbblCheckoutSDK",
                        "PaymentEventBroadCastReceiver: Unknown action: $action"
                    )
                }
                return
            }

            // Handle activity dismissal for both success and failure
            myActivityReference?.get()?.let { _ ->
                dismissReactActivity()
            } ?: run {
                if (is_debug_enabled) {
                    Log.w("NimbblCheckoutSDK", "Activity reference is null or activity is null")
                }
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
                    if (is_debug_enabled) {
                        Log.e(
                            "NimbblCheckoutSDK",
                            "PaymentEventBroadCastReceiver: handlePaymentResult data is null or empty"
                        )
                    }
                    // Log null data event
                    SDKUtils.logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            "error" to "payment_success_data_is_null_or_empty",
                            "location" to "PaymentEventBroadCastReceiver",
                            "error_message" to "PaymentResult data is null or empty",
                            "stack_trace" to SDKUtils.getStackTrace()
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
                    mapOf<String, Any>("payment_data" to data)
                )

                // Check for null listener
                if (listener == null) {
                    if (is_debug_enabled) {
                        Log.e(
                            "NimbblCheckoutSDK",
                            "PaymentEventBroadCastReceiver: listener is null for PaymentResult"
                        )
                    }
                    // Log null listener event
                    SDKUtils.logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            "error" to "listener_is_null_for_payment_success",
                            "location" to "PaymentEventBroadCastReceiver",
                            "error_message" to "Listener is null for PaymentResult",
                            "stack_trace" to SDKUtils.getStackTrace()
                        )
                    )
                } else {
                    try {
                        val parsedData = jsonString2Map(data)
                        listener?.onCheckoutResponse(parsedData)
                    } catch (e: Exception) {
                        if (is_debug_enabled) {
                            Log.e("NimbblCheckoutSDK", "Error parsing payment response data: ${e.message}")
                            Log.e("NimbblCheckoutSDK", "Raw data: ${data.take(200)}...")
                        }
                        // Log parsing error event (non-blocking)
                        SDKUtils.logEventWithVersion(
                            context,
                            EventConstants.EXCEPTION_THROWN,
                            null,
                            null,
                            null,
                            mapOf<String, Any>(
                                "error" to "json_parsing_failed",
                                "location" to "PaymentEventBroadCastReceiver_PaymentSuccess",
                                "error_message" to "Failed to parse payment response JSON",
                                "stack_trace" to SDKUtils.getStackTrace(e)
                            )
                        )
                        // Return empty map as fallback
                        listener?.onCheckoutResponse(mutableMapOf())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Log payment failed event (non-blocking)
                SDKUtils.logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to (e.message ?: "Unknown error"),
                        "location" to "PaymentEventBroadCastReceiver_PaymentSuccess",
                        "error_message" to (e.message ?: "Unknown error"),
                        "stack_trace" to SDKUtils.getStackTrace(e)
                    )
                )
            }
        }

    }



    private fun jsonString2Map(jsonString: String): MutableMap<String, Any> {
        val keys: MutableMap<String, Any> = HashMap()
        
        // Validate input
        if (jsonString.isBlank()) {
            if (is_debug_enabled) {
                Log.w("NimbblCheckoutSDK", "jsonString2Map: Empty or blank JSON string provided")
            }
            return keys
        }
        
        try {
            val jsonObject = JSONObject(jsonString)
            
            // Check if this has a payload field and extract only the payload content
            if (jsonObject.has("payload")) {
                val payloadString = jsonObject.optString("payload", "")
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
                        if (is_debug_enabled) {
                            Log.e("NimbblCheckoutSDK", "jsonString2Map: Error parsing payload JSON: ${e.message}")
                        }
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
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "jsonString2Map: JSON parsing error: ${e.message}")
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "jsonString2Map: Unexpected error: ${e.message}")
            }
        }
        
        return keys
    }

    /**
     * Check if broadcast receiver is already registered
     * @return true if receiver is registered, false otherwise
     */
    private fun isReceiverRegistered(): Boolean {
        return isReceiverRegistered
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
            SDKUtils.logDebug("NimbblCheckoutSDK", "SDK is already cleaned up")
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
                        SDKUtils.logDebug("NimbblCheckoutSDK", "Broadcast receiver unregistered successfully")
                    } catch (e: Exception) {
                        SDKUtils.logError("NimbblCheckoutSDK", "Error unregistering broadcast receiver: ${e.message}", e)
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

            SDKUtils.logDebug("NimbblCheckoutSDK", "SDK cleanup completed successfully")

        } catch (e: Exception) {
            SDKUtils.logError("NimbblCheckoutSDK", "Error during SDK cleanup: ${e.message}", e)

            // Log cleanup failure event using stored context reference
            mContext?.let {
                SDKUtils.logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "sdk_cleanup_failed",
                        "location" to "cleanup_method",
                        "error_message" to (e.message ?: "Unknown cleanup error"),
                        "stack_trace" to SDKUtils.getStackTrace(e)
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