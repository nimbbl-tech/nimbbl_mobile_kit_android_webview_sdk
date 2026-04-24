
package tech.nimbbl.webviewsdk.models

import android.os.Bundle

/**
 * User information passed to Nimbbl Checkout.
 */
class NimbblCheckoutUserInfo {

    companion object {
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_EMAIL = "email"
    }

    var displayName: String? = null
    var email: String? = null

    constructor()
    constructor(b: Bundle) {
        if (b.containsKey(KEY_DISPLAY_NAME)) displayName = b.getString(KEY_DISPLAY_NAME)
        if (b.containsKey(KEY_EMAIL)) email = b.getString(KEY_EMAIL)
    }

    fun asBundle(): Bundle {
        val b = Bundle()
        if (displayName != null) b.putString(KEY_DISPLAY_NAME, displayName)
        if (email != null) b.putString(KEY_EMAIL, email)
        return b
    }
}