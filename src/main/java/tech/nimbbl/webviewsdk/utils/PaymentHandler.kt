package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.net.toUri
import org.json.JSONException
import org.json.JSONObject
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import java.net.URLDecoder
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION

/**
 * Handler class for managing payment success and failure scenarios
 */
class PaymentHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "PaymentHandler"
        private const val RESPONSE_QUERY_PARAM = "response"
        private const val ERROR_SWW_DOT_DOT = "Something went wrong ..."
    }
    
    /**
     * Handle payment success scenario
     * @param payloadJObject JSON object containing payment success data
     * @param orderID Order ID for logging
     * @param orderToken Order token for logging
     */
    fun handlePaymentSuccess(
        payloadJObject: JSONObject,
        orderID: String,
        orderToken: String,
        subMerchantId: String,
    ) {
        try {
            // Log payment status success event
            EventLoggingUtils.safeLogEvent(
                context,
                EventConstants.PAYMENT_STATUS_SUCCESS,
                orderID,
                orderToken,
                subMerchantId,
                mapOf("payload" to payloadJObject.toString()),
                PaymentConstants.SDK_VERSION
            )
            
            // Log payment success event
            EventLoggingUtils.safeLogEvent(
                context,
                "payment_success",
                orderID,
                orderToken,
                subMerchantId,
                mapOf("payload" to payloadJObject.toString()),
                PaymentConstants.SDK_VERSION
            )
            
            // Send success broadcast
            context.sendBroadcast(Intent("PaymentSuccess").apply {
                putExtra("data", payloadJObject.toString())
            })
            
            if (is_debug_enabled) {
                Log.d(TAG, "Payment success handled for order: $orderID")
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error handling payment success: ${e.message}", e)
            }
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                "exception_thrown",
                orderID,
                orderToken,
                subMerchantId,
                mapOf("error" to (e.message ?: "Unknown error")),
                PaymentConstants.SDK_VERSION
            )
            // Send success broadcast
            context.sendBroadcast(Intent("PaymentSuccess").apply {
                putExtra("data", payloadJObject.toString())
            })
        }
    }
    
    /**
     * Handle payment failure scenario
     * @param errorStr Error message
     * @param orderID Order ID for logging
     * @param orderToken Order token for logging
     */
    fun handlePaymentFailure(
        errorStr: String,
        orderID: String,
        orderToken: String,
        subMerchantId: String,
    ) {
        try {
            // Log payment status failed event
            EventLoggingUtils.safeLogEvent(
                context,
                EventConstants.PAYMENT_STATUS_FAILED,
                orderID,
                orderToken,
                subMerchantId,
                mapOf("error_reason" to errorStr),
                PaymentConstants.SDK_VERSION
            )
            
            // Send failure broadcast
            context.sendBroadcast(Intent("PaymentFailure").apply {
                putExtra("data", errorStr)
            })
            
            if (is_debug_enabled) {
                Log.d(TAG, "Payment failure handled for order: $orderID, error: $errorStr")
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error handling payment failure: ${e.message}", e)
            }
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                "exception_thrown",
                orderID,
                orderToken,
                subMerchantId,
                mapOf("error" to (e.message ?: "Unknown error")),
                PaymentConstants.SDK_VERSION
            )
            // Send failure broadcast
            context.sendBroadcast(Intent("PaymentFailure").apply {
                putExtra("data", errorStr)
            })
        }
    }
    
    /**
     * Process callback URL and determine payment status
     * @param url Callback URL to process
     * @param orderID Order ID for logging
     * @param orderToken Order token for logging
     * @return true if URL was processed successfully
     */
    fun processCallbackURL(
        url: String,
        orderID: String,
        orderToken: String,
        subMerchantId: String
    ): Boolean {
        if (url.isEmpty()) {
            if (is_debug_enabled) {
                Log.w(TAG, "Empty URL provided for callback processing")
            }
            return false
        }
        
        if (is_debug_enabled) {
            Log.d(TAG, "Processing callback URL: $url")
        }
        
        // Log redirect URL hit event
        EventLoggingUtils.safeLogEvent(
            context,
            "redirect_url_hit",
            orderID,
            orderToken,
            subMerchantId,
            mapOf("callback_url" to url),
            PaymentConstants.SDK_VERSION
        )
        
        var errorMsg = ERROR_SWW_DOT_DOT
        try {
            val queryParameter = url.toUri().getQueryParameter(RESPONSE_QUERY_PARAM)
            if (!TextUtils.isEmpty(queryParameter)) {
                var isSuccess = false
                var payloadJObject = JSONObject()

                val text = URLDecoder.decode(queryParameter)
                if (is_debug_enabled) {
                    Log.d(TAG, "Decoded response: $text")
                }
                
                try {
                    val jObject = JSONObject(text)
                    if (jObject.has("payload")) {
                        payloadJObject = jObject.getJSONObject("payload")
                        if (payloadJObject.has("status")) {
                            val status = payloadJObject.getString("status")
                            isSuccess = status.lowercase() == "success"
                        }
                    }
                    
                    if (isSuccess) {
                        handlePaymentSuccess(payloadJObject, orderID, orderToken,subMerchantId)
                    } else {
                        if (payloadJObject.has("reason")) {
                            errorMsg = payloadJObject.getString("reason")
                        }
                        handlePaymentFailure(errorMsg, orderID, orderToken,subMerchantId)
                    }
                    
                    return true
                } catch (e: JSONException) {
                    if (is_debug_enabled) {
                        Log.e(TAG, "JSON parsing error: ${e.message}", e)
                    }
                    handlePaymentFailure(errorMsg, orderID, orderToken,subMerchantId)
                    return false
                }
            } else {
                if (is_debug_enabled) {
                    Log.w(TAG, "No response parameter found in callback URL")
                }
                handlePaymentFailure(errorMsg, orderID, orderToken,subMerchantId)
                return false
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error processing callback URL: ${e.message}", e)
            }
            handlePaymentFailure(errorMsg, orderID, orderToken,subMerchantId)
            return false
        }
    }
    

} 