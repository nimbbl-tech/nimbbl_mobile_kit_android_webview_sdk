/*
 * Copyright @ 2019-present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.nimbbl.webviewsdk

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