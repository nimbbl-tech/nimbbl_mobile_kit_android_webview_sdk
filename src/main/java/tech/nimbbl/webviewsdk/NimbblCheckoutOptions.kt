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
import android.os.Parcel
import android.os.Parcelable
import tech.nimbbl.webviewsdk.NimbblCheckoutOptions.Builder

/**
 * This class represents the options when opening Nimbbl checkout. The user can create an
 * instance by using [Builder] and setting the desired options
 * there.
 *
 * The resulting [NimbblCheckoutOptions] object is immutable and represents how the
 * checkout will be opened.
 */
class NimbblCheckoutOptions : Parcelable {
    /**
     * string API Key ID that must generated from the Nimbbl Dashboard.
     */
    var packageName: String? = null
        private set

    /**
     * integer The amount to be paid by the customer in currency.
     */
    var amount = 0
        private set

    /**
     * string The currency in which the payment should be made by the customer.
     */
    var currency: String? = null
        private set

    /**
     * string The merchant/company name shown in the Checkout form..
     */
    var name: String? = null
        private set

    /**
     * string Description of the purchase item shown in the Checkout form. Must start with an alphanumeric character.
     */
    var description: String? = null
        private set

    /**
     * string Link to an image (usually your business logo) shown in the Checkout form. Can also be a base64 string, if loading the image from a network is not desirable.
     */
    var image: String? = null
        private set

    /**
     * string Order ID generated via Orders Api
     */
    var orderId: String? = null
        private set

    /**
     * USer information, to be used when no token is specified.
     */
    var userInfo: NimbblCheckoutUserInfo? = null
        private set

    var subMerchantId: String? = null
        private set

    var token: String? = null
        private set

    var orderToken: String? = null
        private set

    var paymentModeCode: String? = null
        private set

    var bankCode: String? = null
        private set

    var walletCode: String? = null
        private set

    var paymentFlow: String? = null
        private set

    var upiId: String? = null
        private set

    var upiAppCode: String? = null
        private set

    var invoiceId: String? = null
        private set

    /**
     * Class used to build the immutable [NimbblCheckoutOptions] object.
     */
    class Builder {
        private var packageName: String? = null
        private var amount = 0
        private var currency: String? = null
        private var name: String? = null
        private var description: String? = null
        private var image: String? = null
        private var orderID: String? = null
        private var userInfo: NimbblCheckoutUserInfo? = null
        private var subMerchantId: String? = null
        private var token: String? = null
        private var orderToken: String? = null
        private var paymentModeCode: String? = null
        private var bankCode: String? = null
        private var walletCode: String? = null
        private var paymentFlow: String? = null
        private var upiId: String? = null
        private var upiAppCode: String? = null
        private var invoiceId: String? = null

        /**\
         * Sets the server URL.
         * @param key - [URL] of the server where the conference should take place.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setPackageName(key: String?): Builder {
            packageName = key
            return this
        }

        /**
         * Sets the room where the conference will take place.
         * @param amount - Name of the room.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setAmount(amount: Int): Builder {
            this.amount = amount
            return this
        }

        /**
         * Sets the conference subject.
         * @param currency - Subject for the conference.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setCurrency(currency: String?): Builder {
            this.currency = currency
            return this
        }

        /**
         * Sets the JWT token to be used for authentication when joining a conference.
         * @param name - The JWT token to be used for authentication.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        /**
         * Sets the color scheme override so the app is themed. See:
         * for the structure.
         * @param description - A color scheme to be applied to the app.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setDescription(description: String?): Builder {
            this.description = description
            return this
        }

        /**
         * Indicates the conference will be joined with the microphone muted.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setImage(image: String?): Builder {
            this.image = image
            return this
        }

        /**
         * Indicates the conference will be joined in audio-only mode. In this mode no video is
         * sent or received.
         * @return - The [Builder] object itself so the method calls can be chained.
         */
        fun setOrderId(orderId: String?): Builder {
            orderID = orderId
            return this
        }

        fun setUserInfo(userInfo: NimbblCheckoutUserInfo?): Builder {
            this.userInfo = userInfo
            return this
        }

        fun setSubMerchantId(sbMechatId: String?): Builder {
            subMerchantId = sbMechatId
            return this
        }

        fun setWalletCode(wCode: String?): Builder {
            walletCode = wCode
            return this
        }

        fun setPaymentModeCode(paymentCode: String?): Builder {
            paymentModeCode = paymentCode
            return this
        }

        fun setBankCode(bCode: String?): Builder {
            bankCode = bCode
            return this
        }

        fun setPaymentFlow(pFlow: String?): Builder {
            paymentFlow = pFlow
            return this
        }

        fun setUpiId(uId: String?): Builder {
            upiId = uId
            return this
        }

        fun setUpiAppCode(uACode: String?): Builder {
            upiAppCode = uACode
            return this
        }

        fun setToken(tkn: String?): Builder {
            token = tkn
            return this
        }

        fun setOrderToken(tkn: String?): Builder {
            orderToken = tkn
            return this
        }

        fun setInvoiceId(invId: String?): Builder {
            invoiceId = invId
            return this
        }

        /**
         * Builds the immutable [NimbblCheckoutOptions] object with the configuration
         * that this [Builder] instance specified.
         * @return - The built [NimbblCheckoutOptions] object.
         */
        fun build(): NimbblCheckoutOptions {
            val options = NimbblCheckoutOptions()
            options.packageName = packageName
            options.amount = amount
            options.currency = currency
            options.name = name
            options.description = description
            options.image = image
            options.orderId = orderID
            options.userInfo = userInfo
            options.subMerchantId = subMerchantId
            options.token = token
            options.orderToken = orderToken
            options.paymentModeCode = paymentModeCode
            options.bankCode = bankCode
            options.walletCode = walletCode
            options.paymentFlow = paymentFlow
            options.upiId = upiId
            options.upiAppCode = upiAppCode
            options.invoiceId = invoiceId
            return options
        }
    }

    private constructor() {}
    private constructor(`in`: Parcel) {
        packageName = `in`.readString()
        amount = `in`.readInt()
        currency = `in`.readString()
        name = `in`.readString()
        description = `in`.readString()
        image = `in`.readString()
        orderId = `in`.readString()
        userInfo = NimbblCheckoutUserInfo(`in`.readBundle()!!)
        subMerchantId = `in`.readString()
        token = `in`.readString()
        orderToken = `in`.readString()
        paymentModeCode = `in`.readString()
        bankCode = `in`.readString()
        walletCode = `in`.readString()
        paymentFlow = `in`.readString()
        upiId = `in`.readString()
        upiAppCode = `in`.readString()
        invoiceId = `in`.readString()
    }


    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(packageName)
        dest.writeInt(amount)
        dest.writeString(currency)
        dest.writeString(name)
        dest.writeString(description)
        dest.writeString(image)
        dest.writeString(orderId)
        dest.writeBundle(if (userInfo != null) userInfo!!.asBundle() else Bundle())
        dest.writeString(subMerchantId)
        dest.writeString(token)
        dest.writeString(orderToken)
        dest.writeString(paymentModeCode)
        dest.writeString(bankCode)
        dest.writeString(walletCode)
        dest.writeString(paymentFlow)
        dest.writeString(upiId)
        dest.writeString(upiAppCode)
        dest.writeString(invoiceId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NimbblCheckoutOptions> {
        override fun createFromParcel(parcel: Parcel): NimbblCheckoutOptions {
            return NimbblCheckoutOptions(parcel)
        }

        override fun newArray(size: Int): Array<NimbblCheckoutOptions?> {
            return arrayOfNulls(size)
        }
    }

}