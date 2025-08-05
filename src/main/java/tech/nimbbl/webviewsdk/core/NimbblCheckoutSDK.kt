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
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener
import tech.nimbbl.webviewsdk.utils.RestApiUtils.NIMBBL_TECH_URL
import tech.nimbbl.webviewsdk.utils.RestApiUtils.WEB_VIEW_RESP_CHECK_URL
import tech.nimbbl.webviewsdk.utils.RestApiUtils.WEB_VIEW_VIEW_URL
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference


class NimbblCheckoutSDK private constructor() {
    private var myActivityReference : WeakReference<Activity?>? = null
    private var receiver: PaymentEventBroadCastReceiver? = null
    private var isReceiverRegistered = false
    private var listener: NimbblCheckoutPaymentListener? = null
    private var mContext: Context? = null

    fun checkout(options: NimbblCheckoutOptions?) {
        // CHECKOUT_001: Check for null context first (Reference is null)
        if (mContext == null) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "checkout: mContext is null")
            }
            // Call listener with checkout failed
            listener?.onPaymentFailed(createErrorResponse(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_REFERENCE_NULL
            ))
            return
        }

        // CHECKOUT_002: Check for null listener
        if (listener == null) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "checkout: listener is null")
            }
            // Log null listener event
            logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "checkout_listener_is_null",
                    "location" to "checkout_method",
                    "error_message" to PaymentConstants.ERROR_MESSAGE_CHECKOUT_LISTENER_NULL_MERCHANT,
                    "stack_trace" to getStackTrace()
                )
            )
            // Cannot call listener since it's null, just return
            return
        }

        // CHECKOUT_003: Check for null options
        if (options == null) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "checkout: options is null")
            }
            // Log null options event
            logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "checkout_options_is_null",
                    "location" to "checkout_method",
                    "error_message" to PaymentConstants.ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL_MERCHANT,
                    "stack_trace" to getStackTrace()
                )
            )
            // Call listener with checkout failed
            listener?.onPaymentFailed(createErrorResponse(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_OPTIONS_NULL
            ))
            return
        }

        // CHECKOUT_004: Check for null orderToken
        if (options.orderToken.isNullOrEmpty()) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "checkout: orderToken is null or empty")
            }
            // Log null orderToken event
            logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "checkout_token_is_null_or_empty",
                    "location" to "checkout_method",
                    "error_message" to PaymentConstants.ERROR_MESSAGE_CHECKOUT_TOKEN_NULL_MERCHANT,
                    "stack_trace" to getStackTrace()
                )
            )
            // Call listener with checkout failed
            listener?.onPaymentFailed(createErrorResponse(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_TOKEN_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_TOKEN_NULL
            ))
            return
        }

        // CHECKOUT_005 & CHECKOUT_006: Check for null orderId and subMerchantId in token
        try {
            val orderId = parseJwtToken(mContext!!, options.orderToken!!)
            val subMerchantId = getMerchantIDFromJwtToken(mContext!!, options.orderToken!!)
            
            // CHECKOUT_005: Check for null orderId
            if (orderId.isEmpty()) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "checkout: orderId is null or empty in token")
                }
                // Log null orderId event
                logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "checkout_order_id_is_null_or_empty",
                        "location" to "checkout_method",
                        "error_message" to PaymentConstants.ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL_MERCHANT,
                        "stack_trace" to getStackTrace()
                    )
                )
                // Call listener with checkout failed
                listener?.onPaymentFailed(createErrorResponse(
                    PaymentConstants.ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL_MERCHANT,
                    PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL,
                    PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_ORDER_ID_NULL
                ))
                return
            }
            
            // CHECKOUT_006: Check for null subMerchantId
            if (subMerchantId.isEmpty()) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "checkout: subMerchantId is null or empty in token")
                }
                // Log null subMerchantId event
                logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    orderId,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "checkout_submerchant_id_is_null_or_empty",
                        "location" to "checkout_method",
                        "error_message" to PaymentConstants.ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL_MERCHANT,
                        "stack_trace" to getStackTrace()
                    )
                )
                // Call listener with checkout failed
                listener?.onPaymentFailed(createErrorResponse(
                    PaymentConstants.ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL_MERCHANT,
                    PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL,
                    PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL
                ))
                return
            }
            
            // Log checkout initiated event (non-blocking)
            logEventWithVersion(
                mContext!!,
                EventConstants.CHECKOUT_INITIATED,
                orderId,
                options.orderToken,
                subMerchantId
            )
            
        } catch (e: Exception) {
            // Event logging failed but don't impact the main flow
            if (is_debug_enabled) {
                Log.w("NimbblCheckoutSDK", "Event logging failed for checkout_initiated: ${e.message}")
            }
        }

        try {
            
            val intent = Intent(mContext, tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity::class.java)
            intent.putExtra("options", options)
            mContext!!.startActivity(intent)
            
            // Check for null receiver before registering
            if (receiver == null) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "checkout: receiver is null")
                }
                // Log null receiver event
                logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "receiver_is_null",
                        "location" to "checkout_method",
                        "error_message" to "Receiver is null",
                        "stack_trace" to getStackTrace()
                    )
                )
                return
            }
            
            // Use ContextCompat to handle flags automatically for all API levels
            try {
                // Check if receiver is already registered to prevent double registration
                if (mContext != null && !isReceiverRegistered()) {
                    ContextCompat.registerReceiver(
                        mContext!!, receiver, IntentFilter("PaymentSuccess"),
                        ContextCompat.RECEIVER_EXPORTED
                    )
                    ContextCompat.registerReceiver(
                        mContext!!, receiver, IntentFilter("PaymentFailure"),
                        ContextCompat.RECEIVER_EXPORTED
                    )
                    isReceiverRegistered = true
                    if (is_debug_enabled) {
                        Log.d("NimbblCheckoutSDK", "Broadcast receivers registered successfully")
                    }
                } else if (isReceiverRegistered()) {
                    if (is_debug_enabled) {
                        Log.d("NimbblCheckoutSDK", "Broadcast receivers already registered")
                    }
                } else {
                    Log.e("NimbblCheckoutSDK", "Context is null, cannot register receivers")
                }
            } catch (e: Exception) {
                Log.e("NimbblCheckoutSDK", "Error registering broadcast receivers: ${e.message}", e)
                // Log exception event
                logEventWithVersion(
                    mContext!!,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    options.orderToken,
                    null,
                    mapOf<String, Any>(
                        "error" to "broadcast_receiver_registration_failed",
                        "location" to "checkout_method",
                        "error_message" to "Failed to register broadcast receivers",
                        "stack_trace" to getStackTrace(e)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Log exception thrown event (non-blocking)
            logEventWithVersion(
                mContext!!,
                EventConstants.EXCEPTION_THROWN,
                null, // orderId will be extracted from token in MainActivity
                options.orderToken,
                null,
                mapOf<String, Any>(
                    "error" to (e.message ?: "Unknown error"),
                    "location" to "checkout_method",
                    "error_message" to (e.message ?: "Unknown error"),
                    "stack_trace" to getStackTrace(e)
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
                logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "my_activity_reference_is_null",
                        "location" to "dismissReactActivity",
                        "error_message" to "MyActivityReference is null",
                        "stack_trace" to getStackTrace()
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
                listener?.onPaymentFailed(createErrorResponse(
                    PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                    PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                    PaymentConstants.CUSTOMER_MESSAGE_UNKNOWN_ERROR
                ))
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
                listener?.onPaymentFailed(createErrorResponse(
                    PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                    PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                    PaymentConstants.CUSTOMER_MESSAGE_UNKNOWN_ERROR
                ))
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
                listener?.onPaymentFailed(createErrorResponse(
                    PaymentConstants.ERROR_MESSAGE_API_URL_NULL,
                    PaymentConstants.NIMBBL_ERROR_CODE_API_URL_NULL,
                    PaymentConstants.CUSTOMER_MESSAGE_API_URL_NULL
                ))
            }
            return
        }
        
        NIMBBL_TECH_URL = apiUrl
        var webViewUrl = ""
        var webViewRespUrl = ""
        
        // Handle dynamic QA URLs and custom environments (matching iOS implementation)
        when {
            apiUrl.contains("qa1api.nimbbl.tech") -> {
                webViewUrl = "https://qa1sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://qa1sonic.nimbbl.tech/mobile/redirect"
            }
            apiUrl.contains("qa2api.nimbbl.tech") -> {
                webViewUrl = "https://qa2sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://qa2sonic.nimbbl.tech/mobile/redirect"
            }
            apiUrl.contains("qa4api.nimbbl.tech") -> {
              //  webViewUrl = "http://192.168.0.12:4200/?token=%1\$s"
               // webViewRespUrl = "http://192.168.0.12:4200/mobile/redirect"
                webViewUrl = "https://qa4sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://qa4sonic.nimbbl.tech/mobile/redirect"
            }
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

               // webViewUrl = "http://192.168.0.12:4200/?token=%1\$s"
               // webViewRespUrl = "http://192.168.0.12:4200/mobile/redirect"

                webViewUrl = "https://sonicpp.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://sonicpp.nimbbl.tech/mobile/redirect"
            }
            apiUrl.contains("api.nimbbl.tech") && !apiUrl.contains("qa") && !apiUrl.contains("pp") -> {
                // Production (matching iOS)
                webViewUrl = "https://sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://sonic.nimbbl.tech/mobile/redirect"
            }
            else -> {
                // Handle custom environments or unknown URLs (matching iOS fallback)
                if (is_debug_enabled) {
                    Log.w("NimbblCheckoutSDK", "Unknown environment URL: $apiUrl, using production as fallback")
                }
                webViewUrl = "https://sonic.nimbbl.tech/?token=%1\$s"
                webViewRespUrl = "https://sonic.nimbbl.tech/mobile/redirect"
            }
        }
        
        WEB_VIEW_VIEW_URL = webViewUrl
        WEB_VIEW_RESP_CHECK_URL = webViewRespUrl
        
        if (is_debug_enabled) {
            Log.d("NimbblCheckoutSDK", "Environment set - API: $apiUrl, WebView: $webViewUrl, Resp: $webViewRespUrl")
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

    fun init(activity: Activity?) {
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
                        "stack_trace" to getStackTrace()
                    )
                )
            } catch (e: Exception) {
                if (is_debug_enabled) {
                    Log.w("NimbblCheckoutSDK", "Event logging failed for null deviceFingerPrint: ${e.message}")
                }
            }
            // Return initialization error
            listener?.onPaymentFailed(createErrorResponse(
                PaymentConstants.ERROR_MESSAGE_INIT_FAILED,
                PaymentConstants.NIMBBL_ERROR_CODE_INIT_FAILED,
                PaymentConstants.CUSTOMER_MESSAGE_INIT_FAILED
            ))
            return
        }
        
        if (deviceID.isNullOrEmpty()) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "init: deviceID is null or empty")
            }
            // Log null deviceID event
            logEventWithVersion(
                activity,
                EventConstants.EXCEPTION_THROWN,
                null,
                null,
                null,
                mapOf<String, Any>(
                    "error" to "device_id_is_null_or_empty",
                    "location" to "init_method",
                    "error_message" to "Device ID is null or empty",
                    "stack_trace" to getStackTrace()
                )
            )
            // Return initialization error
            listener?.onPaymentFailed(createErrorResponse(
                PaymentConstants.ERROR_MESSAGE_INIT_FAILED,
                PaymentConstants.NIMBBL_ERROR_CODE_INIT_FAILED,
                PaymentConstants.CUSTOMER_MESSAGE_INIT_FAILED
            ))
            return
        }
        
        val fingerPrint = md5(deviceFingerPrint + deviceID)
        NimbblCoreApiSDK.getInstance()?.initialiseAPISDK(NIMBBL_TECH_URL, fingerPrint, deviceFingerPrint)
        
        // Log SDK initialized event (non-blocking)
        logEventWithVersion(
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
            
            // Check for null intent
            if (intent == null) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: intent is null")
                }
                // Log null intent event
                logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "intent_is_null",
                        "location" to "PaymentEventBroadCastReceiver",
                        "error_message" to "Intent is null",
                        "stack_trace" to getStackTrace()
                    )
                )
                return
            }
            
            val action = intent.action
            val data = intent.getStringExtra("data")
            
            // Check for null action
            if (action.isNullOrEmpty()) {
                if (is_debug_enabled) {
                    Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: action is null or empty")
                }
                // Log null action event
                logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "action_is_null_or_empty",
                        "location" to "PaymentEventBroadCastReceiver",
                        "error_message" to "Action is null or empty",
                        "stack_trace" to getStackTrace()
                    )
                )
                return
            }
            
            // Validate action is one of the expected actions
            if (action != "PaymentSuccess" && action != "PaymentFailure") {
                if (is_debug_enabled) {
                    Log.w("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: Unknown action: $action")
                }
                return
            }
            
            // Handle activity dismissal for both success and failure
            myActivityReference?.get()?.let { activity ->
                if (activity != null) {
                    dismissReactActivity()
                }
            } ?: run {
                if (is_debug_enabled) {
                    Log.w("NimbblCheckoutSDK", "Activity reference is null or activity is null")
                }
            }
            
            when (action) {
                "PaymentSuccess" -> handlePaymentSuccess(context, data)
                "PaymentFailure" -> handlePaymentFailure(context, data)
            }
        }
        
        /**
         * Handle payment success
         */
        private fun handlePaymentSuccess(context: Context, data: String?) {
            try {
                // Check for null data
                if (data.isNullOrEmpty()) {
                    if (is_debug_enabled) {
                        Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: PaymentSuccess data is null or empty")
                    }
                    // Log null data event
                    logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            "error" to "payment_success_data_is_null_or_empty",
                            "location" to "PaymentEventBroadCastReceiver",
                            "error_message" to "PaymentSuccess data is null or empty",
                            "stack_trace" to getStackTrace()
                        )
                    )
                    return
                }
                
                // Log payment success event (non-blocking)
                logEventWithVersion(
                    context,
                    EventConstants.PAYMENT_SUCCESS,
                    null,
                    null,
                    null,
                    mapOf<String, Any>("payment_data" to data)
                )
                
                // Check for null listener
                if (listener == null) {
                    if (is_debug_enabled) {
                        Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: listener is null for PaymentSuccess")
                    }
                    // Log null listener event
                    logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            "error" to "listener_is_null_for_payment_success",
                            "location" to "PaymentEventBroadCastReceiver",
                            "error_message" to "Listener is null for PaymentSuccess",
                            "stack_trace" to getStackTrace()
                        )
                    )
                } else {
                    listener?.onPaymentSuccess(jsonString2Map(data))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Log payment failed event (non-blocking)
                logEventWithVersion(
                    context,
                    EventConstants.PAYMENT_FAILED,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to (e.message ?: "Unknown error"),
                        "location" to "PaymentEventBroadCastReceiver_PaymentSuccess",
                        "error_message" to (e.message ?: "Unknown error"),
                        "stack_trace" to getStackTrace(e)
                    )
                )
            }
        }
        
        /**
         * Handle payment failure
         */
        private fun handlePaymentFailure(context: Context, data: String?) {
            try {
                if (data != null) {
                    // Log payment failure event (non-blocking)
                    logEventWithVersion(
                        context,
                        EventConstants.PAYMENT_FAILED,
                        null,
                        null,
                        null,
                        mapOf<String, Any>("failure_reason" to data)
                    )
                    
                    // Check for null listener
                    if (listener == null) {
                        if (is_debug_enabled) {
                            Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: listener is null for PaymentFailure")
                        }
                        // Log null listener event
                        logEventWithVersion(
                            context,
                            EventConstants.EXCEPTION_THROWN,
                            null,
                            null,
                            null,
                            mapOf<String, Any>(
                                "error" to "listener_is_null_for_payment_failure",
                                "location" to "PaymentEventBroadCastReceiver",
                                "error_message" to "Listener is null for PaymentFailure",
                                "stack_trace" to getStackTrace()
                            )
                        )
                    } else {
                        listener?.onPaymentFailed(data)
                    }
                } else {
                    if (is_debug_enabled) {
                        Log.e("NimbblCheckoutSDK", "PaymentEventBroadCastReceiver: PaymentFailure data is null")
                    }
                    // Log null PaymentFailure data event
                    logEventWithVersion(
                        context,
                        EventConstants.EXCEPTION_THROWN,
                        null,
                        null,
                        null,
                        mapOf<String, Any>(
                            "error" to "payment_failure_data_is_null",
                            "location" to "PaymentEventBroadCastReceiver",
                            "error_message" to "PaymentFailure data is null",
                            "stack_trace" to getStackTrace()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Log exception event
                logEventWithVersion(
                    context,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to (e.message ?: "Unknown error"),
                        "location" to "PaymentEventBroadCastReceiver_PaymentFailure",
                        "error_message" to (e.message ?: "Unknown error"),
                        "stack_trace" to getStackTrace(e)
                    )
                )
            }
        }
    }
    
    /**
     * Helper method to log events with webview SDK version
     * @param context The context for logging
     * @param eventName The name of the event to log
     * @param orderId Optional order ID for the event
     * @param token Optional token for the event
     * @param subMerchantId Optional sub merchant ID for the event
     * @param additionalData Optional additional data for the event
     */
    private fun logEventWithVersion(
        context: Context,
        eventName: String,
        orderId: String? = null,
        token: String? = null,
        subMerchantId: String? = null,
        additionalData: Map<String, Any>? = null
    ) {
        try {
            NimbblCoreApiSDK.getInstance()?.logEvent(
                context,
                eventName,
                orderId,
                token,
                subMerchantId,
                additionalData,
                null, // customUserAgent
                null, // customDeviceInfo
                mapOf("appVersion" to PaymentConstants.SDK_VERSION)
            )
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.w("NimbblCheckoutSDK", "Event logging failed for $eventName: ${e.message}")
            }
        }
    }
    
    /**
     * Helper method to create comprehensive error JSON response
     * @param merchantMessage The merchant error message
     * @param nimbblErrorCode The Nimbbl error code
     * @param customerMessage The customer-facing error message
     * @return JSON string with comprehensive error information
     */
    private fun createErrorResponse(
        merchantMessage: String,
        nimbblErrorCode: String,
        customerMessage: String
    ): String {
        return """
            {
                "nimbbl_merchant_message": "$merchantMessage",
                "nimbbl_error_code": "$nimbblErrorCode",
                "nimbbl_consumer_message": "$customerMessage"
            }
        """.trimIndent()
    }
    
    /**
     * Get current stack trace as string
     */
    private fun getStackTrace(): String {
        return getStackTrace(Exception())
    }
    
    /**
     * Get stack trace from exception as string
     */
    private fun getStackTrace(exception: Exception): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        exception.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }
    
    @Throws(JSONException::class)
    private fun jsonString2Map(jsonString: String): MutableMap<String, Any> {
        
        val keys: MutableMap<String, Any> = HashMap()
        try {
            val jsonObject = JSONObject(jsonString) // HashMap
            val keyset: Iterator<*> = jsonObject.keys() // HM
            while (keyset.hasNext()) {
                val key = keyset.next() as String
                val value = jsonObject[key]
                //System.out.print("\n Key : "+key);
                if (value is JSONObject) {
                    //System.out.println("Incomin value is of JSONObject : ");
                    keys[key] = jsonString2Map(value.toString())
                } else {
                    keys[key] = value
                }
            }
        } catch (e: JSONException) {
            if (is_debug_enabled) {
                Log.e("NimbblCheckoutSDK", "jsonString2Map: JSON parsing error: ${e.message}")
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
            if (is_debug_enabled) {
                Log.d("NimbblCheckoutSDK", "SDK is already cleaned up")
            }
            return
        }
        
        try {
            // Unregister broadcast receiver if registered
            if (receiver != null && mContext != null && isReceiverRegistered) {
                try {
                    mContext!!.unregisterReceiver(receiver)
                    isReceiverRegistered = false
                    if (is_debug_enabled) {
                        Log.d("NimbblCheckoutSDK", "Broadcast receiver unregistered successfully")
                    }
                } catch (e: Exception) {
                    Log.w("NimbblCheckoutSDK", "Error unregistering broadcast receiver: ${e.message}", e)
                    isReceiverRegistered = false
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
            
            if (is_debug_enabled) {
                Log.d("NimbblCheckoutSDK", "SDK cleanup completed successfully")
            }
            
            // Log cleanup event
            mContext?.let {
                logEventWithVersion(
                    it,
                    EventConstants.SDK_CLEANUP,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "cleanup_status" to "success",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("NimbblCheckoutSDK", "Error during SDK cleanup: ${e.message}", e)
            
            // Log cleanup failure event
            mContext?.let {
                logEventWithVersion(
                    it,
                    EventConstants.EXCEPTION_THROWN,
                    null,
                    null,
                    null,
                    mapOf<String, Any>(
                        "error" to "sdk_cleanup_failed",
                        "location" to "cleanup_method",
                        "error_message" to (e.message ?: "Unknown cleanup error"),
                        "stack_trace" to getStackTrace(e)
                    )
                )
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: NimbblCheckoutSDK? = null
            get() {
                if (field == null) {
                    field = NimbblCheckoutSDK()
                }
                return field
            }
            private set
    }
}