package tech.nimbbl.webviewsdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.nimbbl.webviewsdk.constants.PaymentConstants

/**
 * Plain JVM tests: createErrorResponse/getStackTrace/debug-logging-flag are pure Kotlin,
 * with no Android API calls, so they don't need a device/emulator.
 */
class SDKUtilsTest {

    @Test
    fun createErrorResponse_mapsEveryKnownErrorCode() {
        val cases = mapOf(
            PaymentConstants.ERROR_CODE_INIT_FAILED to PaymentConstants.NIMBBL_ERROR_CODE_INIT_FAILED,
            PaymentConstants.ERROR_CODE_CHECKOUT_REFERENCE_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_REFERENCE_NULL,
            PaymentConstants.ERROR_CODE_CHECKOUT_LISTENER_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_LISTENER_NULL,
            PaymentConstants.ERROR_CODE_CHECKOUT_OPTIONS_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_OPTIONS_NULL,
            PaymentConstants.ERROR_CODE_CHECKOUT_TOKEN_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_TOKEN_NULL,
            PaymentConstants.ERROR_CODE_CHECKOUT_ORDER_ID_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_ORDER_ID_NULL,
            PaymentConstants.ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL to PaymentConstants.NIMBBL_ERROR_CODE_CHECKOUT_SUBMERCHANT_ID_NULL,
            PaymentConstants.ERROR_CODE_UNKNOWN_ERROR to PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
            PaymentConstants.ERROR_CODE_API_URL_NULL to PaymentConstants.NIMBBL_ERROR_CODE_API_URL_NULL,
        )

        for ((errorCode, expectedNimbblCode) in cases) {
            val response = SDKUtils.createErrorResponse(errorCode)
            assertEquals(
                "wrong nimbbl_error_code for $errorCode",
                expectedNimbblCode,
                response[PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE]
            )
            assertTrue(response.containsKey(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE))
            assertTrue(response.containsKey(PaymentConstants.JSON_KEY_NIMBBL_CONSUMER_MESSAGE))
        }
    }

    @Test
    fun createErrorResponse_unknownCode_fallsBackToUnknownError() {
        val response = SDKUtils.createErrorResponse("NOT_A_REAL_CODE")
        assertEquals(
            PaymentConstants.NIMBBL_ERROR_CODE_UNKNOWN_ERROR,
            response[PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE]
        )
    }

    @Test
    fun getStackTrace_withoutThrowable_returnsNonEmptyTrace() {
        val trace = SDKUtils.getStackTrace()
        assertTrue(trace.isNotEmpty())
        assertTrue(trace.contains("Exception"))
    }

    @Test
    fun getStackTrace_withThrowable_includesMessage() {
        val trace = SDKUtils.getStackTrace(IllegalStateException("boom"))
        assertTrue(trace.contains("boom"))
    }

    @Test
    fun debugLoggingEnabled_overrideWinsOverDefault() {
        SDKUtils.setDebugLoggingEnabled(true)
        assertTrue(SDKUtils.isDebugLoggingEnabled())

        SDKUtils.setDebugLoggingEnabled(false)
        assertFalse(SDKUtils.isDebugLoggingEnabled())

        // Reset to default (falls back to BuildConfig.DEBUG) so this test doesn't leak state.
        SDKUtils.setDebugLoggingEnabled(null)
    }

    @Test
    fun logEventWithVersion_nullContext_doesNotCrash() {
        // Must short-circuit before touching NimbblCoreApi.
        SDKUtils.logEventWithVersion(null, "test_event")
    }
}
