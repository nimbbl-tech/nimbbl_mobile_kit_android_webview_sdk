package tech.nimbbl.webviewsdk.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PaymentHandlerTest {

    private lateinit var context: Context
    private lateinit var paymentHandler: PaymentHandler
    private var receiver: BroadcastReceiver? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        paymentHandler = PaymentHandler(context)
    }

    @After
    fun tearDown() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }

    private fun captureNextBroadcast(): Pair<CountDownLatch, Array<String?>> {
        val latch = CountDownLatch(1)
        val captured = arrayOfNulls<String>(2) // [EXTRA_PAYMENT_DATA, EXTRA_ORDER_ID]
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent) {
                captured[0] = intent.getStringExtra(PaymentConstants.EXTRA_PAYMENT_DATA)
                captured[1] = intent.getStringExtra(PaymentConstants.EXTRA_ORDER_ID)
                latch.countDown()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PaymentConstants.BROADCAST_PAYMENT_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return latch to captured
    }

    @Test
    fun handlePaymentSuccess_broadcastsPayloadAndOrderId() {
        val (latch, captured) = captureNextBroadcast()

        paymentHandler.handlePaymentSuccess(
            """{"status":"success"}""", "order_1", "token_1", "sub_1"
        )

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("""{"status":"success"}""", captured[0])
        assertEquals("order_1", captured[1])
    }

    @Test
    fun handlePaymentFailure_broadcastsErrorPayload() {
        val (latch, captured) = captureNextBroadcast()

        paymentHandler.handlePaymentFailure(
            orderID = "order_2",
            orderToken = "token_2",
            subMerchantId = "sub_2",
            errorStr = "something went wrong",
            isSendEvent = false,
            errorCode = "CUSTOM_001"
        )

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("order_2", captured[1])
        val payload = JSONObject(captured[0]!!)
        assertEquals("something went wrong", payload.getString(PaymentConstants.JSON_KEY_NIMBBL_MERCHANT_MESSAGE))
        assertEquals("CUSTOM_001", payload.getString(PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE))
    }

    @Test
    fun processCallbackURL_emptyUrl_returnsFalse() {
        assertFalse(paymentHandler.processCallbackURL("", "order_3", "token_3", "sub_3"))
    }

    @Test
    fun processCallbackURL_missingResponseParam_returnsFalse() {
        assertFalse(
            paymentHandler.processCallbackURL(
                "https://example.com/callback?foo=bar", "order_4", "token_4", "sub_4"
            )
        )
    }

    @Test
    fun processCallbackURL_withResponseParam_broadcastsSuccessAndReturnsTrue() {
        val (latch, captured) = captureNextBroadcast()

        val result = paymentHandler.processCallbackURL(
            "https://example.com/callback?response=%7B%22status%22%3A%22ok%22%7D",
            "order_5", "token_5", "sub_5"
        )

        assertTrue(result)
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("""{"status":"ok"}""", captured[0])
        assertEquals("order_5", captured[1])
    }
}
