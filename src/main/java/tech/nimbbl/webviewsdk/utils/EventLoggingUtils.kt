package tech.nimbbl.webviewsdk.utils

import android.content.Context
import tech.nimbbl.coreapi.NimbblCoreApi
import tech.nimbbl.coreapi.events.EventConstants
import tech.nimbbl.coreapi.events.NimbblEvent

/**
 * Safe, fire-and-forget event logging for the WebView SDK. Replaces the old Core API SDK's
 * `EventLoggingUtils.safeLogEvent`; [NimbblCoreApi.logEvent] already masks tokens in URL-like
 * fields and drops events logged before initialisation.
 */
internal object EventLoggingUtils {

    private const val TAG = "EventLoggingUtils"

    /**
     * @param context unused by the KMP core; kept so existing call sites read the same.
     * @param sdkVersion reported as the event's app version; falls back to the core SDK version.
     */
    @Suppress("UNUSED_PARAMETER")
    fun safeLogEvent(
        context: Context?,
        eventName: String,
        orderId: String? = null,
        token: String? = null,
        subMerchantId: String? = null,
        additionalData: Map<String, Any>? = null,
        sdkVersion: String? = null
    ) {
        try {
            NimbblCoreApi.logEvent(
                NimbblEvent(
                    name = eventName,
                    orderId = orderId,
                    token = token,
                    subMerchantId = subMerchantId,
                    additionalData = additionalData,
                    customAppInfo = sdkVersion?.takeIf { it.isNotBlank() }
                        ?.let { mapOf(EventConstants.KEY_APP_VERSION to it) }
                )
            )
        } catch (e: Exception) {
            SDKUtils.logWarning(TAG, "Event logging failed for $eventName: ${e.message}")
        }
    }
}
