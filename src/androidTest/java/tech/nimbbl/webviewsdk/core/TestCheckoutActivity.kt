package tech.nimbbl.webviewsdk.core

import android.app.Activity
import tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

/**
 * Minimal real Activity used to exercise [NimbblCheckoutSDK.init]/[NimbblCheckoutSDK.checkout],
 * which require a live Activity implementing [NimbblCheckoutPaymentListener].
 */
class TestCheckoutActivity : Activity(), NimbblCheckoutPaymentListener {

    val receivedResponses = CopyOnWriteArrayList<MutableMap<String, Any>>()
    @Volatile var latch: CountDownLatch? = null

    override fun onCheckoutResponse(data: MutableMap<String, Any>) {
        receivedResponses.add(data)
        latch?.countDown()
    }
}

/** A real Activity that deliberately does NOT implement [NimbblCheckoutPaymentListener]. */
class PlainTestActivity : Activity()
