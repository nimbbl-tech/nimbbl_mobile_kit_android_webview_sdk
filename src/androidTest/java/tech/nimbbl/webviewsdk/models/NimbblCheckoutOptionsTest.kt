package tech.nimbbl.webviewsdk.models

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NimbblCheckoutOptionsTest {

    private fun fullyPopulatedOptions(): NimbblCheckoutOptions =
        NimbblCheckoutOptions.Builder()
            .setPackageName("pkg_123")
            .setAmount(999)
            .setCurrency("INR")
            .setName("Acme Store")
            .setDescription("Order #42")
            .setImage("https://example.com/logo.png")
            .setUserInfo(NimbblCheckoutUserInfo().apply {
                displayName = "Jane Doe"
                email = "jane@example.com"
            })
            .setSubMerchantId("sub_1")
            .setOrderToken("token_abc")
            .setPaymentModeCode("UPI")
            .setBankCode("hdfc")
            .setWalletCode("phonepe")
            .setEMICode("credit")
            .setPaymentFlow("intent")
            .setUpiId("user@upi")
            .setUpiAppCode("phonepeupi")
            .build()

    @Test
    fun builder_setsEveryField() {
        val options = fullyPopulatedOptions()

        assertEquals("pkg_123", options.packageName)
        assertEquals(999, options.amount)
        assertEquals("INR", options.currency)
        assertEquals("Acme Store", options.name)
        assertEquals("Order #42", options.description)
        assertEquals("https://example.com/logo.png", options.image)
        assertEquals("Jane Doe", options.userInfo?.displayName)
        assertEquals("jane@example.com", options.userInfo?.email)
        assertEquals("sub_1", options.subMerchantId)
        assertEquals("token_abc", options.orderToken)
        assertEquals("UPI", options.paymentModeCode)
        assertEquals("hdfc", options.bankCode)
        assertEquals("phonepe", options.walletCode)
        assertEquals("credit", options.emiCode)
        assertEquals("intent", options.paymentFlow)
        assertEquals("user@upi", options.upiId)
        assertEquals("phonepeupi", options.upiAppCode)
    }

    @Test
    fun builder_leavesUnsetFieldsNull() {
        val options = NimbblCheckoutOptions.Builder().build()

        assertNull(options.packageName)
        assertEquals(0, options.amount)
        assertNull(options.userInfo)
        assertNull(options.emiCode)
        assertNull(options.upiAppCode)
    }

    @Test
    fun parcelable_roundTripsEveryField() {
        val original = fullyPopulatedOptions()

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = NimbblCheckoutOptions.CREATOR.createFromParcel(parcel)

            assertEquals(original.packageName, restored.packageName)
            assertEquals(original.amount, restored.amount)
            assertEquals(original.currency, restored.currency)
            assertEquals(original.name, restored.name)
            assertEquals(original.description, restored.description)
            assertEquals(original.image, restored.image)
            assertEquals(original.userInfo?.displayName, restored.userInfo?.displayName)
            assertEquals(original.userInfo?.email, restored.userInfo?.email)
            assertEquals(original.subMerchantId, restored.subMerchantId)
            assertEquals(original.orderToken, restored.orderToken)
            assertEquals(original.paymentModeCode, restored.paymentModeCode)
            assertEquals(original.bankCode, restored.bankCode)
            assertEquals(original.walletCode, restored.walletCode)
            assertEquals(original.emiCode, restored.emiCode)
            assertEquals(original.paymentFlow, restored.paymentFlow)
            assertEquals(original.upiId, restored.upiId)
            assertEquals(original.upiAppCode, restored.upiAppCode)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun parcelable_roundTripsWithNoUserInfo() {
        val original = NimbblCheckoutOptions.Builder().setOrderToken("token_only").build()

        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = NimbblCheckoutOptions.CREATOR.createFromParcel(parcel)

            assertEquals("token_only", restored.orderToken)
            assertNull(restored.userInfo)
        } finally {
            parcel.recycle()
        }
    }
}
