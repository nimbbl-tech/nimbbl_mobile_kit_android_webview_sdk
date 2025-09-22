package tech.nimbbl.webviewsdk.models.interfaces

/*
Created by Sandeep Yadav on 23/02/22.
Copyright (c) 2022 Bigital Technologies Pvt. Ltd. All rights reserved.
*/

/**
 * Interface for listening to events coming from Nimbbl Checkout SDK.
 */
interface NimbblCheckoutPaymentListener {
    /**
     * Called when a conference was joined.
     *
     * @param data Map with a "url" key with the conference URL.
     */
    fun onCheckoutResponse(data: MutableMap<String, Any>)
} 