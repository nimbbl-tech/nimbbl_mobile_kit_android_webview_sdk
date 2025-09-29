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
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BROADCAST_PAYMENT_RESULT
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_PAYMENT_DATA
import tech.nimbbl.webviewsdk.constants.PaymentConstants.RESPONSE_QUERY_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled

/**
 * Handler class for managing payment success and failure scenarios
 */
class PaymentHandler(private val context: Context) {

    companion object {
        private const val TAG = "PaymentHandler"
    }

    /**
     * Handle payment success scenario
     * @param payloadJObject JSON object containing payment success data
     * @param orderID Order ID for logging
     * @param orderToken Order token for logging
     */
    fun handlePaymentSuccess(
        payloadJObject: String,
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
                mapOf("payload" to payloadJObject),
                PaymentConstants.SDK_VERSION
            )

            // Log payment success event
            EventLoggingUtils.safeLogEvent(
                context,
                "payment_success",
                orderID,
                orderToken,
                subMerchantId,
                mapOf("payload" to payloadJObject),
                PaymentConstants.SDK_VERSION
            )

            // Send success broadcast
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                putExtra(EXTRA_PAYMENT_DATA, payloadJObject)
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
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                putExtra(EXTRA_PAYMENT_DATA, payloadJObject)
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
                mapOf("error_reason" to ERROR_MESSAGE_UNKNOWN_ERROR),
                PaymentConstants.SDK_VERSION
            )

            // Send failure broadcast
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                putExtra(
                    "data", """
            {
                "nimbbl_merchant_message": "$ERROR_MESSAGE_UNKNOWN_ERROR",
                "nimbbl_error_code": "$ERROR_CODE_UNKNOWN_ERROR",
                "nimbbl_consumer_message": "$ERROR_MESSAGE_UNKNOWN_ERROR"
            }
        """.trimIndent()
                )
            })

            if (is_debug_enabled) {
                Log.d(
                    TAG,
                    "Payment failure handled for order: $orderID, error: $ERROR_MESSAGE_UNKNOWN_ERROR"
                )
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
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                putExtra(
                    "data", """
            {
                "nimbbl_merchant_message": "$ERROR_MESSAGE_UNKNOWN_ERROR",
                "nimbbl_error_code": "$ERROR_CODE_UNKNOWN_ERROR",
                "nimbbl_consumer_message": "$ERROR_MESSAGE_UNKNOWN_ERROR"
            }
        """.trimIndent()
                )
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

        try {
            val queryParameter = url.toUri().getQueryParameter(RESPONSE_QUERY_PARAM)
            if (!TextUtils.isEmpty(queryParameter)) {

                try {
                    handlePaymentSuccess(queryParameter!!, orderID, orderToken, subMerchantId)
                    return true
                } catch (e: JSONException) {
                    if (is_debug_enabled) {
                        Log.e(TAG, "JSON parsing error: ${e.message}", e)
                    }
                    handlePaymentFailure(orderID, orderToken, subMerchantId)
                    return false
                }
            } else {
                if (is_debug_enabled) {
                    Log.w(TAG, "No response parameter found in callback URL")
                }
                handlePaymentFailure(orderID, orderToken, subMerchantId)
                return false
            }
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e(TAG, "Error processing callback URL: ${e.message}", e)
            }
            handlePaymentFailure(orderID, orderToken, subMerchantId)
            return false
        }
    }


} 