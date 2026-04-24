package tech.nimbbl.webviewsdk.models.interfaces

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

/**
 * Interface for receiving payment results from the Nimbbl Checkout SDK.
 */
interface NimbblCheckoutPaymentListener {
    /**
     * Called when the checkout flow completes — either successfully or with a failure.
     *
     * @param data Map containing the payment result fields (e.g. order status, error codes).
     */
    fun onCheckoutResponse(data: MutableMap<String, Any>)
} 