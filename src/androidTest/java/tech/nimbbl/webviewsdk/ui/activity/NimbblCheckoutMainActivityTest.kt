package tech.nimbbl.webviewsdk.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.coreapisdk.api.RestApiUtils
import tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK
import tech.nimbbl.coreapisdk.core.constants.ServiceConstants
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions
import tech.nimbbl.webviewsdk.testutil.LocalHttpServer
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Covers NimbblCheckoutMainActivity.onCreate's checkout-initiation path: missing-options
 * validation, and the real updateOrder() network round-trip (success + failure), via a local
 * server (see LocalHttpServer) rather than the real Nimbbl backend. Deliberately does NOT assert
 * on what happens after a successful updateOrder (the WebView then loads the checkout page using
 * the OS's own Chromium network stack, not HttpConnectionHelper — out of scope here, and the
 * flakiest possible thing to assert on).
 */
@RunWith(AndroidJUnit4::class)
class NimbblCheckoutMainActivityTest {

    private lateinit var server: LocalHttpServer
    private val originalBaseUrl = ServiceConstants.BASE_URL
    private var receiver: BroadcastReceiver? = null
    private val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        server = LocalHttpServer()
        ServiceConstants.BASE_URL = server.baseUrl
        resetRepositorySingleton()
    }

    @After
    fun tearDown() {
        receiver?.let { appContext.unregisterReceiver(it) }
        receiver = null
        server.close()
        ServiceConstants.BASE_URL = originalBaseUrl
        resetRepositorySingleton()
    }

    /** See NimbblCoreApiSDKRepositoryTest for why this reflection reset is needed. */
    private fun resetRepositorySingleton() {
        val field = runCatching {
            NimbblCoreApiSDK::class.java.getDeclaredField("nimbblApiRepository")
        }.getOrNull() ?: return
        field.isAccessible = true
        field.set(null, null)
    }

    private fun fakeJwt(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.sig"
    }

    private fun captureNextPaymentResultBroadcast(): Pair<CountDownLatch, Array<String?>> {
        val latch = CountDownLatch(1)
        val captured = arrayOfNulls<String>(1)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent) {
                captured[0] = intent.getStringExtra(PaymentConstants.EXTRA_PAYMENT_DATA)
                latch.countDown()
            }
        }
        ContextCompat.registerReceiver(
            appContext, receiver, IntentFilter(PaymentConstants.BROADCAST_PAYMENT_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return latch to captured
    }

    @Test
    fun onCreate_missingOptionsExtra_reportsInputInvalid() {
        val (latch, captured) = captureNextPaymentResultBroadcast()
        val intent = Intent(appContext, NimbblCheckoutMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<NimbblCheckoutMainActivity>(intent).use {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            val payload = JSONObject(captured[0]!!)
            assertEquals("Token not found", payload.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE))
        }
    }

    @Test
    fun onCreate_optionsWithoutOrderToken_reportsInputInvalid() {
        val (latch, captured) = captureNextPaymentResultBroadcast()
        val options = NimbblCheckoutOptions.Builder().build() // no orderToken
        val intent = Intent(appContext, NimbblCheckoutMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(PaymentConstants.EXTRA_TAG_OPTIONS, options)

        ActivityScenario.launch<NimbblCheckoutMainActivity>(intent).use {
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            val payload = JSONObject(captured[0]!!)
            assertEquals("Token not found", payload.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE))
        }
    }

    @Test
    fun onCreate_updateOrderSucceeds_sendsRealPatchRequestWithBearerAuth() {
        server.enqueueResponse(
            LocalHttpServer.CannedResponse(200, "OK", body = """{"order_id":"order_1","status":"created"}""")
        )
        val token = fakeJwt("""{"order_id":"order_1","sub_merchant_id":"sm_1"}""")
        val options = NimbblCheckoutOptions.Builder().setOrderToken(token).build()
        val intent = Intent(appContext, NimbblCheckoutMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(PaymentConstants.EXTRA_TAG_OPTIONS, options)

        ActivityScenario.launch<NimbblCheckoutMainActivity>(intent).use {
            val recorded = server.takeRequest(timeoutSeconds = 10)!!
            assertEquals("PATCH", recorded.method)
            assertTrue(recorded.path.endsWith("order"))
            assertEquals("Bearer $token", recorded.headers["Authorization"])
        }
    }

    @Test
    fun onCreate_updateOrderFails_reportsParsedMerchantMessage() {
        server.enqueueResponse(
            LocalHttpServer.CannedResponse(
                400, "Bad Request",
                body = """{"error":{"nimbbl_merchant_message":"Card declined"}}"""
            )
        )
        val token = fakeJwt("""{"order_id":"order_1","sub_merchant_id":"sm_1"}""")
        val options = NimbblCheckoutOptions.Builder().setOrderToken(token).build()
        val (latch, captured) = captureNextPaymentResultBroadcast()
        val intent = Intent(appContext, NimbblCheckoutMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(PaymentConstants.EXTRA_TAG_OPTIONS, options)

        ActivityScenario.launch<NimbblCheckoutMainActivity>(intent).use {
            assertTrue(latch.await(10, TimeUnit.SECONDS))
            val payload = JSONObject(captured[0]!!)
            assertEquals("Card declined", payload.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE))
        }
    }
}
