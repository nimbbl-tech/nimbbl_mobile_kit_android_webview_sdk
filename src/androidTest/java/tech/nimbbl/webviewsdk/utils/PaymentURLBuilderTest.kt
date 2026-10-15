package tech.nimbbl.webviewsdk.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions

@RunWith(AndroidJUnit4::class)
class PaymentURLBuilderTest {

    private val baseUrl = "https://checkout-qa1.nimbbl.tech/view"

    @Test
    fun upi_app_code_is_appended_for_intent_flow() {
        val options = NimbblCheckoutOptions.Builder()
            .setPaymentModeCode("UPI")
            .setPaymentFlow("intent")
            .setUpiAppCode("phonepeupi")
            .build()

        val url = PaymentURLBuilder.buildPaymentURL(baseUrl, options)

        assertTrue("expected upi_app_code in URL: $url", url.contains("upi_app_code=phonepeupi"))
        assertTrue("expected payment_flow=intent in URL: $url", url.contains("payment_flow=intent"))
    }

    @Test
    fun upi_app_code_is_not_appended_for_collect_flow() {
        val options = NimbblCheckoutOptions.Builder()
            .setPaymentModeCode("UPI")
            .setPaymentFlow("collect")
            .setUpiAppCode("phonepeupi")
            .build()

        val url = PaymentURLBuilder.buildPaymentURL(baseUrl, options)

        assertFalse("upi_app_code should be absent for collect flow: $url", url.contains("upi_app_code"))
        assertTrue("expected payment_flow=collect in URL: $url", url.contains("payment_flow=collect"))
    }

    @Test
    fun upi_app_code_is_not_appended_when_payment_flow_is_unset() {
        val options = NimbblCheckoutOptions.Builder()
            .setPaymentModeCode("UPI")
            .setUpiAppCode("phonepeupi")
            .build()

        val url = PaymentURLBuilder.buildPaymentURL(baseUrl, options)

        assertFalse("upi_app_code should be absent without an intent payment flow: $url", url.contains("upi_app_code"))
    }
}
