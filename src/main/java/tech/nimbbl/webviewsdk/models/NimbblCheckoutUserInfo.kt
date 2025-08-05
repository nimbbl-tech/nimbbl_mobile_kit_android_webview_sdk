
package tech.nimbbl.webviewsdk.models

import android.os.Bundle

/**
 * This class represents user information to be passed to
 */
class NimbblCheckoutUserInfo {
    /**
     * User's display name.
     */
    var displayName: String? = null

    /**
     * User's email address.
     */
    var email: String? = null

    constructor() {}
    constructor(b: Bundle) : super() {
        if (b.containsKey("displayName")) {
            displayName = b.getString("displayName")
        }
        if (b.containsKey("email")) {
            email = b.getString("email")
        }
    }

    fun asBundle(): Bundle {
        val b = Bundle()
        if (displayName != null) {
            b.putString("displayName", displayName)
        }
        if (email != null) {
            b.putString("email", email)
        }
        return b
    }
}