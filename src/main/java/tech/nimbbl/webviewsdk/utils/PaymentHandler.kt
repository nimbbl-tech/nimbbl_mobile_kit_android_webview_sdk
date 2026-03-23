package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.net.toUri
import org.json.JSONException
import org.json.JSONObject
import tech.nimbbl.coreapisdk.core.constants.EventConstants
import tech.nimbbl.coreapisdk.core.constants.EventConstants.EXCEPTION_THROWN
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BROADCAST_PAYMENT_RESULT
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_ORDER_ID
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_ORDER_TOKEN
import tech.nimbbl.webviewsdk.constants.PaymentConstants.EXTRA_PAYMENT_DATA
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_CALLBACK_URL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_CONSUMER_MESSAGE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.RESPONSE_QUERY_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_PAYMENT_HANDLER
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UNKNOWN_ERROR

/**
 * Handler class for managing payment success and failure scenarios
 */
class PaymentHandler(private val context: Context) {

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

            // Send success broadcast
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_PAYMENT_DATA, payloadJObject)
                putExtra(EXTRA_ORDER_ID, orderID)
                putExtra(EXTRA_ORDER_TOKEN, orderToken)
            })

            SDKUtils.logDebug(TAG_PAYMENT_HANDLER, "Payment success handled for order: $orderID")
        } catch (e: Exception) {
            SDKUtils.logError(TAG_PAYMENT_HANDLER, "Error handling payment success: ${e.message}", e)
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EXCEPTION_THROWN,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf("error" to (e.message ?: UNKNOWN_ERROR)),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
            // Send success broadcast
            context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_PAYMENT_DATA, payloadJObject)
                putExtra(EXTRA_ORDER_ID, orderID)
                putExtra(EXTRA_ORDER_TOKEN, orderToken)
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
        errorStr: String,
        isSendEvent:Boolean
    ) {
        if(isSendEvent) {
            // Log payment status failed event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EventConstants.PAYMENT_STATUS,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf("error_reason" to errorStr),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
        }

        // Send failure broadcast (same extras as success for consistent host handling)
        val failurePayload = JSONObject().apply {
            put(JSON_KEY_NIMBBL_MERCHANT_MESSAGE, errorStr)
            put(JSON_KEY_NIMBBL_ERROR_CODE, ERROR_CODE_UNKNOWN_ERROR)
            put(JSON_KEY_NIMBBL_CONSUMER_MESSAGE, ERROR_MESSAGE_UNKNOWN_ERROR)
        }.toString()
        context.sendBroadcast(Intent(BROADCAST_PAYMENT_RESULT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PAYMENT_DATA, failurePayload)
            putExtra(EXTRA_ORDER_ID, orderID)
            putExtra(EXTRA_ORDER_TOKEN, orderToken)
        })

        SDKUtils.logDebug(
            TAG_PAYMENT_HANDLER,
            "Payment failure handled for order: $orderID, error: $errorStr"
        )

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
            SDKUtils.logWarning(TAG_PAYMENT_HANDLER, "Empty URL provided for callback processing")
            return false
        }

        SDKUtils.logDebug(TAG_PAYMENT_HANDLER, "Processing callback URL: $url")

        // Log redirect URL hit event
        EventLoggingUtils.safeLogEvent(
            context,
            eventName = EventConstants.REDIRECT_URL_HIT,
            orderId = orderID,
            orderToken,
            subMerchantId,
            additionalData = mapOf(JSON_KEY_CALLBACK_URL to url),
            sdkVersion = PaymentConstants.SDK_VERSION
        )

        try {
            val queryParameter = url.toUri().getQueryParameter(RESPONSE_QUERY_PARAM)
            if (!TextUtils.isEmpty(queryParameter)) {

                try {
                    handlePaymentSuccess(queryParameter!!, orderID, orderToken, subMerchantId)
                    return true
                } catch (e: JSONException) {
                    SDKUtils.logError(TAG_PAYMENT_HANDLER, "JSON parsing error: ${e.message}", e)
                    // Log exception event
                    EventLoggingUtils.safeLogEvent(
                        context,
                        eventName = EXCEPTION_THROWN,
                        orderId = orderID,
                        orderToken,
                        subMerchantId,
                        additionalData = mapOf("error" to (e.message ?: UNKNOWN_ERROR)),
                        sdkVersion = PaymentConstants.SDK_VERSION
                    )
                    return false
                }
            } else {
                SDKUtils.logWarning(TAG_PAYMENT_HANDLER, "No response parameter found in callback URL")
                // Log exception event
                EventLoggingUtils.safeLogEvent(
                    context,
                    eventName = EXCEPTION_THROWN,
                    orderId = orderID,
                    orderToken,
                    subMerchantId,
                    additionalData = mapOf("error" to ("No response parameter found in callback URL")),
                    sdkVersion = PaymentConstants.SDK_VERSION
                )
                return false
            }
        } catch (e: Exception) {
            SDKUtils.logError(TAG_PAYMENT_HANDLER, "Error processing callback URL: ${e.message}", e)
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EXCEPTION_THROWN,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf("error" to (e.message ?: UNKNOWN_ERROR)),
                sdkVersion = PaymentConstants.SDK_VERSION
            )
            return false
        }
    }


} 