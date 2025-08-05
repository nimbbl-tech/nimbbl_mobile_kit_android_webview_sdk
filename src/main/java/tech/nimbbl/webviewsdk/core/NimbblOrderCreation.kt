package tech.nimbbl.webviewsdk.core

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

import retrofit2.Response
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.webviewsdk.models.CreateOrderResponse

/**
 * Order creation functionality for the Nimbbl WebView SDK
 * This delegates to the Core API SDK for order creation
 */
object NimbblOrderCreation {
    
    /**
     * Create a new order
     * @param shopBaseUrl The shop base URL for order creation
     * @param totalAmount The total amount for the order
     * @param emailId Customer email ID
     * @param firstName Customer first name
     * @param mobileNumber Customer mobile number
     * @param productId Product ID
     * @param paymentMode Payment mode (optional, defaults to "All")
     * @param subPaymentMode Sub payment mode (optional)
     * @return Response containing the created order details
     */
    suspend fun createOrder(
        shopBaseUrl: String,
        totalAmount: Int,
        emailId: String,
        firstName: String,
        mobileNumber: String,
        productId: String,
        paymentMode: String = "All",
        subPaymentMode: String? = null
    ): Response<CreateOrderResponse>? {
        val coreResponse = NimbblCoreApiSDK.createOrder(
            shopBaseUrl,
            totalAmount,
            emailId,
            firstName,
            mobileNumber,
            productId,
            paymentMode,
            subPaymentMode
        )
        
        return coreResponse?.let { response ->
            if (response.isSuccessful) {
                val coreBody = response.body()
                if (coreBody != null) {
                    // Convert Core API SDK response to WebView SDK response
                    // This ensures the Sample App only sees WebView SDK models
                    val webviewBody = CreateOrderResponse(
                        amountBeforeTax = coreBody.amountBeforeTax,
                        attempts = coreBody.attempts,
                        currency = coreBody.currency,
                        invoiceId = coreBody.invoiceId,
                        orderDate = coreBody.orderDate,
                        orderId = coreBody.orderId,
                        refreshToken = coreBody.refreshToken,
                        refreshTokenExpiration = coreBody.refreshTokenExpiration,
                        status = coreBody.status,
                        tax = coreBody.tax,
                        token = coreBody.token,
                        tokenExpiration = coreBody.tokenExpiration,
                        totalAmount = coreBody.totalAmount
                    )
                    Response.success(webviewBody)
                } else {
                    Response.success(null)
                }
            } else {
                Response.error(response.code(), response.errorBody()!!)
            }
        }
    }
} 