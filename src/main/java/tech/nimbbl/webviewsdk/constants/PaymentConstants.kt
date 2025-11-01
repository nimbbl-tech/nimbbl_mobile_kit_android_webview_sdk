package tech.nimbbl.webviewsdk.constants

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import android.util.Log
import tech.nimbbl.webviewsdk.BuildConfig

/**
 * Constants used throughout the payment flow
 */
object PaymentConstants {
    
    // Debug flag - automatically false in release builds
    val is_debug_enabled = BuildConfig.DEBUG

    const val SDK_VERSION: String = BuildConfig.SDK_VERSION

    
    // SDK Initialization Errors
    const val ERROR_CODE_INIT_FAILED = "INIT_001"
    const val ERROR_MESSAGE_INIT_FAILED = "SDK Initialise Failed, Pass a valid reference and retry"
    const val NIMBBL_ERROR_CODE_INIT_FAILED = "MERCHANT_SDK_REFERENCE_INVALID"
    const val CUSTOMER_MESSAGE_INIT_FAILED = "There is some technical issue, kindly reach out to the merchant"
    
    // Generic Errors
    const val ERROR_CODE_UNKNOWN_ERROR = "UNKNOWN_001"
    const val ERROR_MESSAGE_UNKNOWN_ERROR = "There seems to be some issue , Please try after sometime."
    const val NIMBBL_ERROR_CODE_UNKNOWN_ERROR = "MERCHANT_SDK_ERROR"
    const val CUSTOMER_MESSAGE_UNKNOWN_ERROR = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_API_URL_NULL = "API_URL_001"
    const val ERROR_MESSAGE_API_URL_NULL = "API URL is null or empty. Please provide a valid environment URL."
    const val NIMBBL_ERROR_CODE_API_URL_NULL = "MERCHANT_SDK_ERROR"
    const val CUSTOMER_MESSAGE_API_URL_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    // Checkout Start Errors
    const val ERROR_CODE_CHECKOUT_REFERENCE_NULL = "CHECKOUT_001"
    const val ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL_MERCHANT = "Checkout Failed, reference element is missing"
    const val NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL = "MERCHANT_SDK_REFERENCE_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_REFERENCE_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_CHECKOUT_LISTENER_NULL = "CHECKOUT_002"
    const val ERROR_MESSAGE_CHECKOUT_LISTENER_NULL_MERCHANT = "Checkout listener is missing, initialize SDK properly"
    const val NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL = "MERCHANT_SDK_REFERENCE_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_LISTENER_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_CHECKOUT_OPTIONS_NULL = "CHECKOUT_003"
    const val ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL_MERCHANT = "Checkout options missing , pass a valid option object"
    const val NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL = "MERCHANT_SDK_OPTIONS_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_OPTIONS_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_CHECKOUT_TOKEN_NULL = "CHECKOUT_004"
    const val ERROR_MESSAGE_CHECKOUT_TOKEN_NULL_MERCHANT = "Checkout token is missing, pass valid options.token and retry"
    const val NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL = "MERCHANT_SDK_TOKEN_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_TOKEN_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_CHECKOUT_ORDER_ID_NULL = "CHECKOUT_005"
    const val ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL_MERCHANT = "Order ID missing inside checkout token."
    const val NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL = "MERCHANT_SDK_ORDERID_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_ORDER_ID_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    const val ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL = "CHECKOUT_006"
    const val ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL_MERCHANT = "Sub-merchant id missing"
    const val NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL = "MERCHANT_SDK_SMID_MISSING"
    const val CUSTOMER_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL = "There is some technical issue, kindly reach out to the merchant"
    
    // Activity extras
    const val EXTRA_TAG_OPTIONS = "options"
    
    // URL parameters
    const val RESPONSE_QUERY_PARAM = "response"
    const val BROWSER_BACK_PARAM = "browserBack"
    
    // Error messages
    const val ERROR_SWW_DOT_DOT = "Something went wrong ..."
    const val ERROR_UNABLE_TO_LAUNCH_UPI = "Unable to launch UPI app."
    
    // JavaScript interface
    const val JAVASCRIPT_INTERFACE_NAME = "NimbblSDK"
    
    // Broadcast actions
    const val BROADCAST_PAYMENT_RESULT = "PaymentResult"

    // Broadcast extras
    const val EXTRA_PAYMENT_DATA = "data"
    
    // SDK actions
    const val SDK_ACTION_DEVICE_BACK_INITIATED = "device_back_initiated"
    const val SDK_SOURCE_ANDROID = "nimbbl_android_sdk"
    
    // UPI intent responses
    const val UPI_INTENT_BACK_RESPONSE_CLOSE = "close"

    
    // WebView JavaScript functions
    const val JS_FUNCTION_POST_MESSAGE = "window.postMessage"
    
    // Log tags
    const val TAG_MAIN_ACTIVITY = "NimbblCheckoutMain"
    const val TAG_BACK_HANDLER = "BackHandler"
    const val TAG_SAN = "SAN"
    
    // Debug log prefixes
    const val DEBUG_ORDER_ID_PREFIX = "orderID-->"
    const val DEBUG_BASE_URL_PREFIX = "BaseURL"
    
    // URL building
    const val URL_PARAM_PAYMENT_MODE = "payment_mode"
    const val URL_PARAM_BANK_CODE = "bank_code"
    const val URL_PARAM_WALLET_CODE = "wallet_code"
    const val URL_PARAM_PAYMENT_FLOW = "payment_flow"
    
    // UPI parameters
    const val UPI_PARAM_URL = "url"
    const val UPI_PARAM_PACKAGE_NAME = "package_name"
    const val UPI_PARAM_TRANSACTION_ID = "transaction_id"
    const val UPI_PARAM_APP_NAME = "upi_app_name"
    const val UPI_PARAM_APP_ICON = "appiconbase64"
    
    // JSON keys
    const val JSON_KEY_UPI_APPS = "UPIApps"
    const val JSON_KEY_PAYLOAD = "payload"
    const val JSON_KEY_STATUS = "status"
    const val JSON_KEY_REASON = "reason"
    const val JSON_KEY_ERROR = "error"
    const val JSON_KEY_NIMBBL_MERCHANT_MESSAGE = "nimbbl_merchant_message"
    
    // Status values
    const val STATUS_SUCCESS = "success"
    
    // Callback modes
    const val CALLBACK_MODE_MOBILE = "callback_mobile"
    const val PLATFORM_TYPE_ANDROID = "android"
}
