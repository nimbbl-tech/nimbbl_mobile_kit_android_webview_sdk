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
    fun onPaymentSuccess(data: MutableMap<String, Any>)

    /**
     * Called when the active conference ends, be it because of user choice or
     * because of a failure.
     *
     * @param data Map with an "error" key with the error and a "url" key with
     * the conference URL. If the conference finished gracefully no `error`
     * key will be present. The possible values for "error" are described here:
     * https://github.com/jitsi/lib-jitsi-meet/blob/master/JitsiConnectionErrors.js
     * https://github.com/jitsi/lib-jitsi-meet/blob/master/JitsiConferenceErrors.js
     */
    fun onPaymentFailed(data: String)
} 