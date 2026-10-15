package tech.nimbbl.webviewsdk.core

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import tech.nimbbl.webviewsdk.constants.PaymentConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NimbblCheckoutSDKTest {

    @Test
    fun getInstance_returnsSameSingleton() {
        assertSame(NimbblCheckoutSDK.getInstance(), NimbblCheckoutSDK.getInstance())
    }

    @Test
    fun init_withNullActivity_doesNotCrash() {
        // Must not throw.
        NimbblCheckoutSDK.getInstance().init(null)
    }

    @Test
    fun checkout_withNullOptionsAfterInit_reportsOptionsNullError() {
        ActivityScenario.launch(TestCheckoutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                NimbblCheckoutSDK.getInstance().init(activity)
                activity.receivedResponses.clear()

                NimbblCheckoutSDK.getInstance().checkout(null)

                assertEquals(1, activity.receivedResponses.size)
                assertEquals(
                    "MERCHANT_SDK_OPTIONS_MISSING",
                    activity.receivedResponses[0][PaymentConstants.JSON_KEY_NIMBBL_ERROR_CODE]
                )

                NimbblCheckoutSDK.getInstance().cleanup()
            }
        }
    }

    @Test
    fun cleanup_unregistersReceiver_soLatePaymentBroadcastIsIgnored() {
        // Broadcast delivery to a main-thread-registered receiver requires the main looper to be
        // free, so the sdk calls + latch waits below run on this (instrumentation) thread, not
        // inside onActivity{} (which executes synchronously on the main thread and would deadlock).
        var activity: TestCheckoutActivity? = null
        val scenario = ActivityScenario.launch(TestCheckoutActivity::class.java)
        scenario.onActivity { activity = it }

        val sdk = NimbblCheckoutSDK.getInstance()
        sdk.init(activity)
        activity!!.receivedResponses.clear()

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // First broadcast, while still registered: should be delivered.
        var latch = CountDownLatch(1)
        activity!!.latch = latch
        context.sendBroadcast(
            Intent(PaymentConstants.BROADCAST_PAYMENT_RESULT)
                .setPackage(context.packageName)
                .putExtra(PaymentConstants.EXTRA_PAYMENT_DATA, """{"status":"success"}""")
                .putExtra(PaymentConstants.EXTRA_ORDER_ID, "order_1")
        )
        assertTrue("expected the first broadcast to be delivered", latch.await(5, TimeUnit.SECONDS))
        assertEquals(1, activity!!.receivedResponses.size)

        sdk.cleanup()

        // Second broadcast, after cleanup(): must be ignored (receiver unregistered).
        latch = CountDownLatch(1)
        activity!!.latch = latch
        context.sendBroadcast(
            Intent(PaymentConstants.BROADCAST_PAYMENT_RESULT)
                .setPackage(context.packageName)
                .putExtra(PaymentConstants.EXTRA_PAYMENT_DATA, """{"status":"success"}""")
                .putExtra(PaymentConstants.EXTRA_ORDER_ID, "order_2")
        )
        assertFalse(
            "payment broadcast must not be delivered after cleanup()",
            latch.await(2, TimeUnit.SECONDS)
        )
        assertEquals(1, activity!!.receivedResponses.size)

        scenario.close()
    }

    @Test
    fun init_withActivityNotImplementingListener_throws() {
        ActivityScenario.launch(PlainTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                try {
                    NimbblCheckoutSDK.getInstance().init(activity)
                    fail("expected IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    // expected
                }
            }
        }
    }
}
