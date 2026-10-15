package tech.nimbbl.webviewsdk.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener

@RunWith(AndroidJUnit4::class)
class ValidationUtilsTest {

    private val listener = object : NimbblCheckoutPaymentListener {
        override fun onCheckoutResponse(data: MutableMap<String, Any>) {}
    }

    @Test
    fun validateCheckoutOptions_nullOptions_isError() {
        val result = ValidationUtils.validateCheckoutOptions(null)
        assertTrue(result is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateCheckoutOptions_missingOrderToken_isError() {
        val options = NimbblCheckoutOptions.Builder().build()
        val result = ValidationUtils.validateCheckoutOptions(options)
        assertTrue(result is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateCheckoutOptions_withOrderToken_isSuccess() {
        val options = NimbblCheckoutOptions.Builder().setOrderToken("tok_123").build()
        val result = ValidationUtils.validateCheckoutOptions(options)
        assertTrue(result is ValidationUtils.ValidationResult.Success)
    }

    @Test
    fun validateContext_null_isError() {
        assertTrue(ValidationUtils.validateContext(null) is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateContext_nonNull_isSuccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(ValidationUtils.validateContext(context) is ValidationUtils.ValidationResult.Success)
    }

    @Test
    fun validateListener_null_isError() {
        assertTrue(ValidationUtils.validateListener(null) is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateListener_nonNull_isSuccess() {
        assertTrue(ValidationUtils.validateListener(listener) is ValidationUtils.ValidationResult.Success)
    }

    @Test
    fun validateOrderId_nullOrEmpty_isError() {
        assertTrue(ValidationUtils.validateOrderId(null) is ValidationUtils.ValidationResult.Error)
        assertTrue(ValidationUtils.validateOrderId("") is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateOrderId_nonEmpty_isSuccess() {
        assertTrue(ValidationUtils.validateOrderId("order_1") is ValidationUtils.ValidationResult.Success)
    }

    @Test
    fun validateSubMerchantId_nullOrEmpty_isError() {
        assertTrue(ValidationUtils.validateSubMerchantId(null) is ValidationUtils.ValidationResult.Error)
        assertTrue(ValidationUtils.validateSubMerchantId("") is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateSubMerchantId_nonEmpty_isSuccess() {
        assertTrue(ValidationUtils.validateSubMerchantId("sub_1") is ValidationUtils.ValidationResult.Success)
    }

    @Test
    fun validateCheckoutPrerequisites_stopsAtFirstFailure_context() {
        val options = NimbblCheckoutOptions.Builder().setOrderToken("tok").build()
        val result = ValidationUtils.validateCheckoutPrerequisites(null, listener, options)
        assertTrue(result is ValidationUtils.ValidationResult.Error)
    }

    @Test
    fun validateCheckoutPrerequisites_allValid_isSuccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = NimbblCheckoutOptions.Builder().setOrderToken("tok").build()
        val result = ValidationUtils.validateCheckoutPrerequisites(context, listener, options)
        assertTrue(result is ValidationUtils.ValidationResult.Success)
    }
}
