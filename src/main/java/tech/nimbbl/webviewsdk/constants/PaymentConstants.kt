package tech.nimbbl.webviewsdk.constants

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import tech.nimbbl.webviewsdk.BuildConfig

/**
 * Constants used throughout the payment flow
 */
object PaymentConstants {
    

    const val SDK_VERSION: String = BuildConfig.SDK_VERSION

    
    // SDK Initialization Errors
    const val ERROR_CODE_INIT_FAILED = "INIT_001"
    const val ERROR_MESSAGE_INIT_FAILED = "SDK Initialise Failed, Pass a valid reference and retry"
    const val NIMBBL_ERROR_CODE_INIT_FAILED = "MERCHANT_SDK_REFERENCE_INVALID"
    const val ERROR_CODE_UNKNOWN_ERROR = "UNKNOWN_001"
    const val ERROR_MESSAGE_UNKNOWN_ERROR = "There seems to be some issue , Please try after sometime."
    const val NIMBBL_ERROR_CODE_UNKNOWN_ERROR = "MERCHANT_SDK_ERROR"
    const val ERROR_CODE_API_URL_NULL = "API_URL_001"
    const val ERROR_MESSAGE_API_URL_NULL = "API URL is null or empty. Please provide a valid environment URL."
    const val NIMBBL_ERROR_CODE_API_URL_NULL = "MERCHANT_SDK_ERROR"

    // Checkout Start Errors
    const val ERROR_CODE_CHECKOUT_REFERENCE_NULL = "CHECKOUT_001"
    const val ERROR_MESSAGE_CHECKOUT_REFERENCE_NULL = "Checkout Failed, reference element is missing"
    const val NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL = "MERCHANT_SDK_REFERENCE_MISSING"
    const val ERROR_CODE_CHECKOUT_LISTENER_NULL = "CHECKOUT_002"
    const val ERROR_MESSAGE_CHECKOUT_LISTENER_NULL = "Checkout listener is missing, initialize SDK properly"
    const val NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL = "MERCHANT_SDK_REFERENCE_MISSING"
    const val ERROR_CODE_CHECKOUT_OPTIONS_NULL = "CHECKOUT_003"
    const val ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL = "Checkout options missing, pass a valid option object"
    const val NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL = "MERCHANT_SDK_OPTIONS_MISSING"
    const val ERROR_CODE_CHECKOUT_TOKEN_NULL = "CHECKOUT_004"
    const val ERROR_MESSAGE_CHECKOUT_TOKEN_NULL = "Checkout token is missing, pass valid options.token and retry"
    const val NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL = "MERCHANT_SDK_TOKEN_MISSING"
    const val ERROR_CODE_CHECKOUT_ORDER_ID_NULL = "CHECKOUT_005"
    const val ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL = "There is some technical issue, kindly reach out to the merchant"
    const val NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL = "MERCHANT_SDK_ORDERID_MISSING"
    const val ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL = "CHECKOUT_006"
    const val ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL = "There is some technical issue, kindly reach out to the merchant"
    const val NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL = "MERCHANT_SDK_SMID_MISSING"

    // Generic Error Messages
    const val ERROR_MESSAGE_GENERIC = "There is some technical issue, kindly reach out to the merchant"

    // UPI Error Messages
    const val ERROR_UNABLE_TO_LAUNCH_UPI = "Unable to launch UPI app."

    // SDK Actions
    const val SDK_ACTION_DEVICE_BACK_INITIATED = "device_back_initiated"
    const val SDK_SOURCE_ANDROID = "nimbbl_android_sdk"

    // UPI Intent Responses
    const val UPI_INTENT_BACK_RESPONSE_CLOSE = "close"

    // Activity extras
    const val EXTRA_TAG_OPTIONS = "options"
    const val EXTRA_ORDER_ID = "order_id"

    // URL parameters
    const val RESPONSE_QUERY_PARAM = "response"
    const val BROWSER_BACK_PARAM = "browserBack"

    // JavaScript interface
    const val JAVASCRIPT_INTERFACE_NAME = "NimbblSDK"
    
    // Broadcast actions
    const val BROADCAST_PAYMENT_RESULT = "PaymentResult"
    const val ACTION_PAYMENT_RESULT = "PaymentResult"

    // Broadcast extras
    const val EXTRA_PAYMENT_DATA = "data"
    const val EXTRA_ORDER_TOKEN = "order_token"

    // JSON payload field
    const val JSON_PAYLOAD_FIELD = "payload"
    const val APP_VERSION_KEY = "appVersion"
    const val SDK_VERSION_KEY = "sdk_version"

    // WebView JavaScript functions
    const val JS_FUNCTION_POST_MESSAGE = "window.postMessage"
    
    // Log tags
    const val TAG_MAIN_ACTIVITY = "NimbblCheckoutMain"
    const val TAG_BACK_HANDLER = "BackHandler"
    const val TAG_SAN = "SAN"
    const val TAG_CHECKOUT_SDK = "NimbblCheckoutSDK"
    const val TAG_PAYMENT_HANDLER = "PaymentHandler"
    const val TAG_PAYMENT_URL_BUILDER = "PaymentURLBuilder"
    const val TAG_SDK_UTILS = "SDKUtils"
    const val TAG_WEBVIEW = "WebViewManager"

    // Debug log prefixes

    // Event Logging Keys - for structured event data
    const val EVENT_KEY_ERROR = "error"
    const val EVENT_KEY_LOCATION = "location"
    const val EVENT_KEY_ERROR_MESSAGE = "error_message"
    const val EVENT_KEY_STACK_TRACE = "stack_trace"
    const val EVENT_KEY_PAYMENT_DATA = "payment_data"

    // Event Error Values
    const val EVENT_ERROR_CHECKOUT_VALIDATION_FAILED = "checkout_validation_failed"
    const val EVENT_ERROR_ORDER_ID_VALIDATION_FAILED = "checkout_order_id_validation_failed"
    const val EVENT_ERROR_SUBMERCHANT_ID_VALIDATION_FAILED = "checkout_submerchant_id_validation_failed"
    const val EVENT_ERROR_RECEIVER_NULL = "receiver_is_null"
    const val EVENT_ERROR_BROADCAST_REGISTRATION_FAILED = "broadcast_receiver_registration_failed"
    const val EVENT_ERROR_MY_ACTIVITY_REF_NULL = "my_activity_reference_is_null"
    const val EVENT_ERROR_DEVICE_FINGERPRINT_NULL = "device_fingerprint_is_null_or_empty"
    const val EVENT_ERROR_DEVICE_ID_NULL = "device_id_is_null_or_empty"
    const val EVENT_ERROR_ACTION_NULL = "action_is_null_or_empty"
    const val EVENT_ERROR_PAYMENT_DATA_NULL = "payment_success_data_is_null_or_empty"
    const val EVENT_ERROR_LISTENER_NULL = "listener_is_null_for_payment_success"
    const val EVENT_ERROR_JSON_PARSING_FAILED = "json_parsing_failed"
    const val EVENT_ERROR_SDK_CLEANUP_FAILED = "sdk_cleanup_failed"

    // Event Location Values
    const val EVENT_LOCATION_CHECKOUT_METHOD = "checkout_method"
    const val EVENT_LOCATION_DISMISS_ACTIVITY = "dismissReactActivity"
    const val EVENT_LOCATION_INIT_METHOD = "init_method"
    const val EVENT_LOCATION_BROADCAST_RECEIVER = "PaymentEventBroadCastReceiver"
    const val EVENT_LOCATION_PAYMENT_SUCCESS = "PaymentEventBroadCastReceiver_PaymentSuccess"
    const val EVENT_LOCATION_CLEANUP_METHOD = "cleanup_method"

    // Event Error Messages
    const val EVENT_MSG_RECEIVER_NULL = "Receiver is null"
    const val EVENT_MSG_BROADCAST_REG_FAILED = "Failed to register broadcast receivers"
    const val EVENT_MSG_MY_ACTIVITY_REF_NULL = "MyActivityReference is null"
    const val EVENT_MSG_DEVICE_FINGERPRINT_NULL = "Device fingerprint is null or empty"
    const val EVENT_MSG_DEVICE_ID_NULL = "Device ID is null or empty"
    const val EVENT_MSG_ACTION_NULL = "Action is null or empty"
    const val EVENT_MSG_PAYMENT_DATA_NULL = "PaymentResult data is null or empty"
    const val EVENT_MSG_LISTENER_NULL = "Listener is null for PaymentResult"
    const val EVENT_MSG_JSON_PARSING_FAILED = "Failed to parse payment response JSON"
    const val EVENT_MSG_UNKNOWN_ERROR = "Unknown error"
    const val EVENT_MSG_UNKNOWN_CLEANUP_ERROR = "Unknown cleanup error"

    // Log Messages - for debug/error/warning logs
    const val LOG_MSG_CHECKOUT_VALIDATION_FAILED = "checkout validation failed: %s"
    const val LOG_MSG_ORDER_ID_VALIDATION_FAILED = "checkout: orderId validation failed: %s"
    const val LOG_MSG_SUBMERCHANT_ID_VALIDATION_FAILED = "checkout: subMerchantId validation failed: %s"
    const val LOG_MSG_EVENT_LOGGING_FAILED_CHECKOUT = "Event logging failed for checkout_initiated: %s"
    const val LOG_MSG_RECEIVER_NULL = "checkout: receiver is null"
    const val LOG_MSG_BROADCAST_REG_SUCCESS = "Broadcast receivers registered successfully"
    const val LOG_MSG_BROADCAST_ALREADY_REGISTERED = "Broadcast receivers already registered"
    const val LOG_MSG_CONTEXT_NULL_REGISTER = "Context is null, cannot register receivers"
    const val LOG_MSG_ERROR_REGISTERING_RECEIVER = "Error registering broadcast receivers: %s"
    const val LOG_MSG_DISMISS_ACTIVITY_REF_NULL = "dismissReactActivity: myActivityReference is null"
    const val LOG_MSG_ERROR_FINISHING_ACTIVITY = "Error finishing activity: %s"
    const val LOG_MSG_DISMISS_ACTIVITY_NULL = "dismissReactActivity: activity is null"
    const val LOG_MSG_SET_ACTIVITY_REF_NULL = "setMyActivityWeakReference: activity is null"
    const val LOG_MSG_ENV_URL_NULL = "setEnvironmentUrl: apiUrl is null or empty"
    const val LOG_MSG_ENV_SET = "Environment set - API: %s, WebView: %s, Resp: %s"
    const val LOG_MSG_INIT_ACTIVITY_NULL = "activity can't be null. returning."
    const val LOG_MSG_INIT_FINGERPRINT_NULL = "init: deviceFingerPrint is null or empty"
    const val LOG_MSG_EVENT_LOGGING_FAILED_FINGERPRINT = "Event logging failed for null deviceFingerPrint: %s"
    const val LOG_MSG_INIT_DEVICE_ID_NULL = "init: deviceID is null or empty"
    const val LOG_MSG_RECEIVER_CONTEXT_NULL = "PaymentEventBroadCastReceiver: context is null"
    const val LOG_MSG_RECEIVER_ACTION_NULL = "PaymentEventBroadCastReceiver: action is null or empty"
    const val LOG_MSG_RECEIVER_UNKNOWN_ACTION = "PaymentEventBroadCastReceiver: Unknown action: %s"
    const val LOG_MSG_ACTIVITY_REF_NULL = "Activity reference is null or activity is null"
    const val LOG_MSG_PAYMENT_DATA_NULL = "PaymentEventBroadCastReceiver: handlePaymentResult data is null or empty"
    const val LOG_MSG_LISTENER_NULL_PAYMENT = "PaymentEventBroadCastReceiver: listener is null for PaymentResult"
    const val LOG_MSG_ERROR_PARSING_PAYMENT = "Error parsing payment response data: %s"
    const val LOG_MSG_RAW_DATA = "Raw data: %s..."
    const val LOG_MSG_JSON_STRING_EMPTY = "jsonString2Map: Empty or blank JSON string provided"
    const val LOG_MSG_ERROR_PARSING_PAYLOAD = "jsonString2Map: Error parsing payload JSON: %s"
    const val LOG_MSG_JSON_PARSING_ERROR = "jsonString2Map: JSON parsing error: %s"
    const val LOG_MSG_UNEXPECTED_ERROR = "jsonString2Map: Unexpected error: %s"
    const val LOG_MSG_SDK_ALREADY_CLEANED = "SDK is already cleaned up"
    const val LOG_MSG_BROADCAST_UNREG_SUCCESS = "Broadcast receiver unregistered successfully"
    const val LOG_MSG_ERROR_UNREG_RECEIVER = "Error unregistering broadcast receiver: %s"
    const val LOG_MSG_CLEANUP_SUCCESS = "SDK cleanup completed successfully"
    const val LOG_MSG_ERROR_DURING_CLEANUP = "Error during SDK cleanup: %s"

    // Debug log prefixes (continued)
    const val DEBUG_ORDER_ID_PREFIX = "orderID-->"
    const val DEBUG_BASE_URL_PREFIX = "BaseURL"
    
    // Update order / callback constants
    const val CALLBACK_MODE_MOBILE = "callback_mobile"
    const val PLATFORM_TYPE_ANDROID = "android"

    // UPI intent parameter keys (from WebView JS)
    const val UPI_PARAM_URL = "url"
    const val UPI_PARAM_PACKAGE_NAME = "package_name"
    const val UPI_PARAM_TRANSACTION_ID = "transaction_id"

    // URL parameter names
    const val PAYMENT_MODE_PARAM = "payment_mode"
    const val BANK_CODE_PARAM = "bank_code"
    const val WALLET_CODE_PARAM = "wallet_code"
    const val PAYMENT_FLOW_PARAM = "payment_flow"

    // JSON keys
    const val JSON_KEY_ERROR = "error"
    const val JSON_KEY_NIMBBL_MERCHANT_MESSAGE = "nimbbl_merchant_message"
    const val JSON_KEY_NIMBBL_ERROR_CODE = "nimbbl_error_code"
    const val JSON_KEY_NIMBBL_CONSUMER_MESSAGE = "nimbbl_consumer_message"
    const val JSON_KEY_ERROR_REASON = "error_reason"
    const val JSON_KEY_CALLBACK_URL = "callback_url"
    const val JSON_KEY_LOCATION = "location"
    const val JSON_KEY_BASE_URL = "base_url"
    const val JSON_KEY_CURRENT_URL = "current_url"

    // Generic error message
    const val UNKNOWN_ERROR = "Unknown error"

    // Error location names for logging
    const val LOCATION_PAYMENT_URL_BUILDER_BROWSER_BACK = "PaymentURLBuilder.buildBrowserBackURL"
    const val LOCATION_UPI_PARAMETER_VALIDATION = "UPI parameter validation"

    // Event logging field names
    const val JSON_KEY_PARAMETERS = "parameters"
    const val JSON_KEY_UPI_APP_NAME = "upi_app_name"
    const val JSON_KEY_UPI_APPS = "UPIApps"
}

