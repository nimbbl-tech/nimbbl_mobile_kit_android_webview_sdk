package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.content.Context
import android.util.Log
import tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils
import tech.nimbbl.webviewsdk.constants.PaymentConstants.SDK_VERSION
import tech.nimbbl.webviewsdk.constants.PaymentConstants.is_debug_enabled
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
        // Validate base URL
        if (baseUrl.isNullOrEmpty()) {
            Log.e("PaymentURLBuilder", "Base URL is null or empty")
            throw IllegalArgumentException("Base URL cannot be null or empty")
        }
        
        // Validate URL format
        if (!isValidUrl(baseUrl)) {
            Log.e("PaymentURLBuilder", "Invalid base URL format: $baseUrl")
            throw IllegalArgumentException("Invalid URL format: $baseUrl")
        }
        
        var url = baseUrl
        
        // Add payment mode if specified
        val paymentModeCode = options.paymentModeCode
        if (!paymentModeCode.isNullOrEmpty()) {
            url = appendParameter(url, "payment_mode", paymentModeCode)
        }
        
        // Add bank code if specified
        val bankCode = options.bankCode
        if (!bankCode.isNullOrEmpty()) {
            url = appendParameter(url, "bank_code", bankCode)
        }
        
        // Add wallet code if specified
        val walletCode = options.walletCode
        if (!walletCode.isNullOrEmpty()) {
            url = appendParameter(url, "wallet_code", walletCode)
        }
        
        // Add payment flow if specified
        val paymentFlow = options.paymentFlow
        if (!paymentFlow.isNullOrEmpty()) {
            url = appendParameter(url, "payment_flow", paymentFlow)
        }
        
        // Validate final URL
        if (!isValidUrl(url)) {
            Log.e("PaymentURLBuilder", "Generated URL is invalid: $url")
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
            Log.w("PaymentURLBuilder", "URL validation failed for: $url", e)
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
        // Validate inputs
        if (key.isNullOrEmpty()) {
            Log.w("PaymentURLBuilder", "Parameter key is null or empty")
            return url
        }
        
        if (value.isNullOrEmpty()) {
            Log.w("PaymentURLBuilder", "Parameter value is null or empty for key: $key")
            return url
        }
        
        return try {
            if (url.contains("?")) {
                "$url&$key=$value"
            } else {
                "$url?$key=$value"
            }
        } catch (e: Exception) {
            Log.e("PaymentURLBuilder", "Error appending parameter: ${e.message}", e)
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
            val uri = android.net.Uri.parse(currentUrl)
            val baseUri = android.net.Uri.parse(baseUrl)
            
            return baseUri.buildUpon().apply {
                uri.queryParameterNames.forEach { param ->
                    appendQueryParameter(
                        param, 
                        if (param == "browserBack") "true" 
                        else uri.getQueryParameter(param)
                    )
                }
                if (!uri.queryParameterNames.contains("browserBack")) {
                    appendQueryParameter("browserBack", "true")
                }
            }.build().toString()
        } catch (e: Exception) {
            if (is_debug_enabled) {
                Log.e("PaymentURLBuilder", "Error building browser back URL: ${e.message}", e)
            }
            // Log exception event
            EventLoggingUtils.safeLogEvent(
                context, // context not available in this utility class
                "exception_thrown",
                orderID, // orderId not available
                orderToken, // token not available
                subMerchantId, // subMerchantId not available
                mapOf<String, Any>(
                    "error" to (e.message ?: "Unknown error"),
                    "location" to "PaymentURLBuilder.buildBrowserBackURL",
                    "base_url" to baseUrl,
                    "current_url" to currentUrl
                ),
                SDK_VERSION
            )
            // Return original base URL as fallback
            baseUrl
        }
    }
} 