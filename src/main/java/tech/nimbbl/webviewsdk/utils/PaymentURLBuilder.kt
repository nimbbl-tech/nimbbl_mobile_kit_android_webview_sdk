package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import tech.nimbbl.coreapisdk.core.constants.EventConstants.EXCEPTION_THROWN
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BANK_CODE_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.BROWSER_BACK_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_BASE_URL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_CURRENT_URL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_LOCATION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.LOCATION_PAYMENT_URL_BUILDER_BROWSER_BACK
import tech.nimbbl.webviewsdk.constants.PaymentConstants.PAYMENT_FLOW_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.PAYMENT_MODE_PARAM
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.TAG_PAYMENT_URL_BUILDER
import tech.nimbbl.webviewsdk.constants.PaymentConstants.UNKNOWN_ERROR
import tech.nimbbl.webviewsdk.constants.PaymentConstants.WALLET_CODE_PARAM
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions

/**
 * Utility class for building payment URLs with proper parameter handling
 */
object PaymentURLBuilder {
    
    /**
     * Build payment URL with validation
     * @param baseUrl The base URL
     * @param options Checkout options
     * @return Validated payment URL
     */
    fun buildPaymentURL(baseUrl: String, options: NimbblCheckoutOptions): String {
        if (baseUrl.isEmpty()) {
            SDKUtils.logError(TAG_PAYMENT_URL_BUILDER, "Base URL is null or empty")
            throw IllegalArgumentException("Base URL cannot be null or empty")
        }
        
        // Validate URL format
        if (!isValidUrl(baseUrl)) {
            SDKUtils.logError(TAG_PAYMENT_URL_BUILDER, "Invalid base URL format: $baseUrl")
            throw IllegalArgumentException("Invalid URL format: $baseUrl")
        }
        
        var url = baseUrl

        // Add payment mode if specified
        val paymentModeCode = options.paymentModeCode
        if (!paymentModeCode.isNullOrEmpty()) {
            url = appendParameter(url, PAYMENT_MODE_PARAM, paymentModeCode)
        }

        // Add bank code if specified
        val bankCode = options.bankCode
        if (!bankCode.isNullOrEmpty()) {
            url = appendParameter(url, BANK_CODE_PARAM, bankCode)
        }

        // Add wallet code if specified
        val walletCode = options.walletCode
        if (!walletCode.isNullOrEmpty()) {
            url = appendParameter(url, WALLET_CODE_PARAM, walletCode)
        }

        // Add payment flow if specified
        val paymentFlow = options.paymentFlow
        if (!paymentFlow.isNullOrEmpty()) {
            url = appendParameter(url, PAYMENT_FLOW_PARAM, paymentFlow)
        }

        // Validate final URL
        if (!isValidUrl(url)) {
            SDKUtils.logError(TAG_PAYMENT_URL_BUILDER, "Generated URL is invalid: $url")
            throw IllegalArgumentException("Generated URL is invalid: $url")
        }
        
        return url
    }
    
    /**
     * Validate URL format
     * @param url URL to validate
     * @return true if URL is valid
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.scheme != null && uri.host != null
        } catch (e: Exception) {
            SDKUtils.logWarning(TAG_PAYMENT_URL_BUILDER, "URL validation failed for: $url")
            false
        }
    }
    
    /**
     * Append a parameter to the URL with proper separator handling and validation
     * @param url Current URL
     * @param key Parameter key
     * @param value Parameter value
     * @return URL with parameter appended
     */
    private fun appendParameter(url: String, key: String, value: String): String {
        if (key.isEmpty()) {
            SDKUtils.logWarning(TAG_PAYMENT_URL_BUILDER, "Parameter key is empty")
            return url
        }
        if (value.isEmpty()) {
            SDKUtils.logWarning(TAG_PAYMENT_URL_BUILDER, "Parameter value is empty for key: $key")
            return url
        }
        
        return try {
            val encodedKey = Uri.encode(key)
            val encodedValue = Uri.encode(value)
            if (url.contains("?")) {
                "$url&$encodedKey=$encodedValue"
            } else {
                "$url?$encodedKey=$encodedValue"
            }
        } catch (e: Exception) {
            SDKUtils.logError(TAG_PAYMENT_URL_BUILDER, "Error appending parameter: ${e.message}", e)
            url // Return original URL as fallback
        }
    }
    
    /**
     * Build URL with browser back parameter for navigation
     * @param context Context
     * @param baseUrl The base URL
     * @param currentUrl Current WebView URL
     * @param orderID Order ID
     * @param orderToken Order token
     * @param subMerchantId Sub-merchant ID
     * @return URL with browser back parameter
     */
    fun buildBrowserBackURL(
        context: Context,
        baseUrl: String,
        currentUrl: String,
        orderID: String,
        orderToken: String?,
        subMerchantId: String
    ): String {
        return try {
            val uri = currentUrl.toUri()
            val baseUri = baseUrl.toUri()
            // Start from base (scheme + host + path), then merge params:
            // currentUrl params take precedence; base-only params are added if not overridden;
            // BROWSER_BACK_PARAM is always set to "true".
            val currentParams = uri.queryParameterNames
            val builder = baseUri.buildUpon().clearQuery()
            // Add base params not present in currentUrl
            baseUri.queryParameterNames.forEach { param ->
                if (param !in currentParams) {
                    builder.appendQueryParameter(param, baseUri.getQueryParameter(param))
                }
            }
            // Add all currentUrl params (BROWSER_BACK_PARAM forced to "true")
            currentParams.forEach { param ->
                builder.appendQueryParameter(
                    param,
                    if (param == BROWSER_BACK_PARAM) "true" else uri.getQueryParameter(param)
                )
            }
            if (BROWSER_BACK_PARAM !in currentParams) {
                builder.appendQueryParameter(BROWSER_BACK_PARAM, "true")
            }
            builder.build().toString()
        } catch (e: Exception) {
            SDKUtils.logError(TAG_PAYMENT_URL_BUILDER, "Error building browser back URL: ${e.message}", e)
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context,
                eventName = EXCEPTION_THROWN,
                orderId = orderID,
                orderToken,
                subMerchantId,
                additionalData = mapOf<String, Any>(
                    JSON_KEY_ERROR to (e.message ?: UNKNOWN_ERROR),
                    JSON_KEY_LOCATION to LOCATION_PAYMENT_URL_BUILDER_BROWSER_BACK,
                    JSON_KEY_BASE_URL to baseUrl,
                    JSON_KEY_CURRENT_URL to currentUrl
                ),
                sdkVersion = SDK_VERSION
            )
            // Return original base URL as fallback
            baseUrl
        }
    }
} 