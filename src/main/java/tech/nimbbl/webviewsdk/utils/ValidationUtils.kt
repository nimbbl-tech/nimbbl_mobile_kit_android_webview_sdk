package tech.nimbbl.webviewsdk.utils

/*
Created by Sandeep Yadav on 23/07/25.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_CODE_INIT_FAILED
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_LISTENER_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_CHECKOUT_TOKEN_NULL
import tech.nimbbl.webviewsdk.constants.PaymentConstants.ERROR_MESSAGE_INIT_FAILED
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions

/**
 * Utility class for input validation
 */
object ValidationUtils {
    
    /**
     * Validation result sealed class
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String, val errorType: String) : ValidationResult()
    }
    
    /**
     * Validate checkout options
     * @param options Checkout options to validate
     * @return Validation result
     */
    fun validateCheckoutOptions(options: NimbblCheckoutOptions?): ValidationResult {
        return when {
            options == null -> ValidationResult.Error(
                ERROR_MESSAGE_CHECKOUT_OPTIONS_NULL,
                ERROR_CODE_CHECKOUT_OPTIONS_NULL
            )
            options.orderToken.isNullOrEmpty() -> ValidationResult.Error(
                ERROR_MESSAGE_CHECKOUT_TOKEN_NULL,
                ERROR_CODE_CHECKOUT_TOKEN_NULL
            )
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate context
     * @param context Context to validate
     * @return Validation result
     */
    fun validateContext(context: Any?): ValidationResult {
        return when {
            context == null -> ValidationResult.Error(
                ERROR_MESSAGE_INIT_FAILED,
                ERROR_CODE_INIT_FAILED
            )
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate listener
     * @param listener Listener to validate
     * @return Validation result
     */
    fun validateListener(listener: Any?): ValidationResult {
        return when {
            listener == null -> ValidationResult.Error(
                ERROR_MESSAGE_CHECKOUT_LISTENER_NULL,
                ERROR_CODE_CHECKOUT_LISTENER_NULL
            )
            else -> ValidationResult.Success
        }
    }

    
    /**
     * Validate order ID
     * @param orderId Order ID to validate
     * @return Validation result
     */
    fun validateOrderId(orderId: String?): ValidationResult {
        return when {
            orderId.isNullOrEmpty() -> ValidationResult.Error(
                ERROR_MESSAGE_CHECKOUT_ORDER_ID_NULL,
                ERROR_CODE_CHECKOUT_ORDER_ID_NULL
            )
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate sub merchant ID
     * @param subMerchantId Sub merchant ID to validate
     * @return Validation result
     */
    fun validateSubMerchantId(subMerchantId: String?): ValidationResult {
        return when {
            subMerchantId.isNullOrEmpty() -> ValidationResult.Error(
                ERROR_MESSAGE_CHECKOUT_SUBMERCHANT_ID_NULL,
                ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL
            )
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate all checkout prerequisites
     * @param context Context
     * @param listener Listener
     * @param options Checkout options
     * @return Validation result
     */
    fun validateCheckoutPrerequisites(
        context: Any?,
        listener: Any?,
        options: NimbblCheckoutOptions?
    ): ValidationResult {
        // Validate context
        validateContext(context).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        // Validate listener
        validateListener(listener).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        // Validate options
        validateCheckoutOptions(options).let { result ->
            if (result is ValidationResult.Error) return result
        }
        
        return ValidationResult.Success
    }
}


