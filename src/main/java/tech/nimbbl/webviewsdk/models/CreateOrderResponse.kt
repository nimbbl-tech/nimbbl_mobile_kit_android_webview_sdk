package tech.nimbbl.webviewsdk.models

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

/**
 * CreateOrderResponse model for the WebView SDK
 * This is completely independent and doesn't depend on Core API SDK models
 */
data class CreateOrderResponse(
    val amountBeforeTax: Int,
    val attempts: Int,
    val currency: String,
    val invoiceId: String,
    val orderDate: String,
    val orderId: String,
    val refreshToken: String,
    val refreshTokenExpiration: String,
    val status: String,
    val tax: Int,
    val token: String,
    val tokenExpiration: String,
    val totalAmount: Int
) 