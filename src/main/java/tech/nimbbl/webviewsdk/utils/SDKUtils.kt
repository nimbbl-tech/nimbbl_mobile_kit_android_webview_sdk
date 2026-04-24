package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.util.Log
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.webviewsdk.BuildConfig
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants.APP_VERSION_KEY
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_REFERENCE_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_INIT_FAILED
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_GENERIC
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_CONSUMER_MESSAGE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_API_URL_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_INIT_FAILED
import tech.nimbbl.webviewsdk.constants.PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_SDK_UTILS
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Utility class for common SDK operations and error handling
 */
object SDKUtils {

    @Volatile
    private var debugLoggingEnabledOverride: Boolean? = null

    fun setDebugLoggingEnabled(enabled: Boolean?) {
        debugLoggingEnabledOverride = enabled
    }

    fun isDebugLoggingEnabled(): Boolean {
        return debugLoggingEnabledOverride ?: BuildConfig.DEBUG
    }

    /**
     * Log error message with consistent formatting
     * @param tag Log tag
     * @param message Error message
     * @param throwable Optional throwable
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Log debug message with consistent formatting
     * @param tag Log tag
     * @param message Debug message
     */
    fun logDebug(tag: String, message: String) {
        if (isDebugLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    /**
     * Log warning message with consistent formatting
     * @param tag Log tag
     * @param message Warning message
     */
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
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
                NIMBBL_ERROR_CODE_INIT_FAILED,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_REFERENCE_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_LISTENER_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_LISTENER_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_OPTIONS_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_TOKEN_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_TOKEN_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_ORDER_ID_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL -> createErrorMap(
                ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL,
                NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_UNKNOWN_ERROR -> createErrorMap(
                PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                ERROR_MESSAGE_GENERIC
            )

            ERROR_CODE_API_URL_NULL -> createErrorMap(
                ERROR_MESSAGE_API_URL_NULL,
                NIMBBL_ERROR_CODE_API_URL_NULL,
                ERROR_MESSAGE_GENERIC
            )

            else -> {
                createErrorMap(
                    PaymentConstants.ERROR_MESSAGE_UNKNOWN_ERROR,
                    NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
                    ERROR_MESSAGE_GENERIC
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
            JSON_KEY_NIMBBL_MERCHANT_MESSAGE to merchantMessage,
            JSON_KEY_NIMBBL_ERROR_CODE to errorCode,
            JSON_KEY_NIMBBL_CONSUMER_MESSAGE to customerMessage
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
                    customAppInfo = mapOf(APP_VERSION_KEY to PaymentConstants.SDK_VERSION)
                )
            }
        } catch (e: Exception) {
            logError(TAG_SDK_UTILS, "Event logging failed for $eventName: ${e.message}", e)
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

}
