package tech.nimbbl.webviewsdk.core

import tech.nimbbl.coreapi.env.NimbblEnvironment

/**
 * URLs for the active environment, set by [NimbblCheckoutSDK.setEnvironmentUrl].
 *
 * Kept here rather than read from `NimbblCoreApi.config` because an IP-based URL points the
 * API at QA3 but the WebView at the IP host, so the two base URLs can differ.
 * Replaces the old Core API SDK's mutable `RestApiUtils` globals (production by default).
 */
internal object CheckoutEnvironment {

    @Volatile
    var apiBaseUrl: String = NimbblEnvironment.PRODUCTION_API_URL

    /** Checkout URL with [NimbblEnvironment.TOKEN_PLACEHOLDER] in place of the order token. */
    @Volatile
    var webViewUrlTemplate: String = NimbblEnvironment.getWebViewUrlTemplate(NimbblEnvironment.PRODUCTION_API_URL)

    @Volatile
    var webViewResponseUrl: String = NimbblEnvironment.getWebViewResponseUrl(NimbblEnvironment.PRODUCTION_API_URL)

    fun webViewUrl(token: String): String =
        webViewUrlTemplate.replace(NimbblEnvironment.TOKEN_PLACEHOLDER, token)
}
