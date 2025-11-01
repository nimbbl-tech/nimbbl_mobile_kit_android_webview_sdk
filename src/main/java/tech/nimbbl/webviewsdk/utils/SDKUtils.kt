package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.util.Log
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_REFERENCE_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_INIT_FAILED
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Utility class for common SDK operations and error handling
 */
object SDKUtils {

    /**
     * Log error with consistent formatting
     * @param tag Log tag
     * @param message Error message
     * @param throwable Optional throwable
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (is_debug_enabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    /**
     * Log warning with consistent formatting
     * @param tag Log tag
     * @param message Warning message
     */
    fun logWarning(tag: String, message: String) {
        if (is_debug_enabled) {
            Log.w(tag, message)
        }
    }

    /**
     * Log debug message with consistent formatting
     * @param tag Log tag
     * @param message Debug message
     */
    fun logDebug(tag: String, message: String) {
        if (is_debug_enabled) {
            Log.d(tag, message)
        }
    }

    /**
     * Create comprehensive error response
     * @param errorType Type of error
     * @return Error response map
     */
    fun createErrorResponse(errorType: String): MutableMap<String, Any> {
        return when (errorType) {
            ERROR_CODE_INIT_FAILED -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_INIT_FAILED,
                PaymentConstants.NIMBBL_ERROR_CODE_INIT_FAILED,
                PaymentConstants.CUSTOMER_MESSAGE_INIT_FAILED
            )

            ERROR_CODE_CHECKOUT_REFERENCE_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_REFERENCE_NULL
            )

            ERROR_CODE_CHECKOUT_LISTENER_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_LISTENER_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_LISTENER_NULL
            )

            ERROR_CODE_CHECKOUT_OPTIONS_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_OPTIONS_NULL
            )

            ERROR_CODE_CHECKOUT_TOKEN_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_TOKEN_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_TOKEN_NULL
            )

            ERROR_CODE_CHECKOUT_ORDER_ID_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_ORDER_ID_NULL
            )

            ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL_MERCHANT,
                PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL
            )

            ERROR_CODE_API_URL_NULL -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_API_URL_NULL,
                PaymentConstants.NIMBBL_ERROR_CODE_API_URL_NULL,
                PaymentConstants.CUSTOMER_MESSAGE_API_URL_NULL
            )

            ERROR_CODE_UNKNOWN_ERROR -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                PaymentConstants.CUSTOMER_MESSAGE_UNKNOWN_ERROR
            )

            else -> {
                createErrorMap(
                    PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                    PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                    PaymentConstants.CUSTOMER_MESSAGE_UNKNOWN_ERROR
                )
            }
        }
    }

    /**
     * Create error map from individual components
     */
    private fun createErrorMap(
        merchantMessage: String,
        errorCode: String,
        customerMessage: String
    ): MutableMap<String, Any> {
        return mutableMapOf(
            "nimbbl_merchant_message" to merchantMessage,
            "nimbbl_error_code" to errorCode,
            "nimbbl_consumer_message" to customerMessage
        )
    }

    /**
     * Log event with version information
     * @param context Context for logging
     * @param eventName Event name
     * @param orderId Optional order ID
     * @param token Optional token
     * @param subMerchantId Optional sub merchant ID
     * @param additionalData Optional additional data
     */
    fun logEventWithVersion(
        context: Context?,
        eventName: String,
        orderId: String? = null,
        token: String? = null,
        subMerchantId: String? = null,
        additionalData: Map<String, Any>? = null
    ) {
        try {
            context?.let {
                NimbblCoreApiSDK.getInstance()?.logEvent(
                    it,
                    eventName,
                    orderId,
                    token,
                    subMerchantId,
                    additionalData,
                    customUserAgent = null, // customUserAgent
                    customDeviceInfo = null, // customDeviceInfo
                    customAppInfo = mapOf("appVersion" to PaymentConstants.SDK_VERSION)
                )
            }
        } catch (e: Exception) {
            logError("SDKUtils", "Event logging failed for $eventName: ${e.message}", e)
        }
    }

    /**
     * Get stack trace as string
     * @param throwable Optional throwable, if null creates new Exception
     * @return Stack trace string
     */
    fun getStackTrace(throwable: Throwable? = null): String {
        val exception = throwable ?: Exception()
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        exception.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    /**
     * Check if string is null or empty
     * @param value String to check
     * @return true if null or empty
     */
    fun isNullOrEmpty(value: String?): Boolean {
        return value.isNullOrEmpty()
    }

    /**
     * Check if context is valid
     * @param context Context to check
     * @return true if context is valid
     */
    fun isValidContext(context: Context?): Boolean {
        return context != null
    }
}
