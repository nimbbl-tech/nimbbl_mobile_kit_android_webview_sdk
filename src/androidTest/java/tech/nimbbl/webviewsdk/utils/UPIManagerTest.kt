package tech.nimbbl.webviewsdk.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.webviewsdk.constants.PaymentConstants.JSON_KEY_UPI_APPS
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class UPIManagerTest {

    private val upiManager = UPIManager(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun createUPIAppsJSON_returnsJsonWithUpiAppsArray() {
        val results = ArrayBlockingQueue<org.json.JSONObject>(1)
        upiManager.createUPIAppsJSON { results.add(it) }

        val result = results.poll(5, TimeUnit.SECONDS)
        assertTrue("expected a result before timeout", result != null)
        assertTrue(result!!.has(JSON_KEY_UPI_APPS))
        // The emulator has no UPI apps installed; the array must still be present (possibly empty).
        assertTrue(result.getJSONArray(JSON_KEY_UPI_APPS).length() >= 0)
    }

    @Test
    fun validateUPIParameters_allFieldsPresent_isValid() {
        val params = """{"url":"upi://pay?x=1","package_name":"com.phonepe.app","transaction_id":"txn_1"}"""
        assertTrue(upiManager.validateUPIParameters(params, "order_1", "token_1", "sub_1"))
    }

    @Test
    fun validateUPIParameters_missingField_isInvalid() {
        val params = """{"url":"upi://pay?x=1","package_name":"com.phonepe.app"}"""
        assertFalse(upiManager.validateUPIParameters(params, "order_1", "token_1", "sub_1"))
    }

    @Test
    fun validateUPIParameters_emptyFieldValue_isInvalid() {
        val params = """{"url":"","package_name":"com.phonepe.app","transaction_id":"txn_1"}"""
        assertFalse(upiManager.validateUPIParameters(params, "order_1", "token_1", "sub_1"))
    }

    @Test
    fun validateUPIParameters_malformedJson_isInvalid() {
        assertFalse(upiManager.validateUPIParameters("not json", "order_1", "token_1", "sub_1"))
    }
}
