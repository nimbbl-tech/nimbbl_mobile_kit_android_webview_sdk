package tech.nimbbl.webviewsdk.utils

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WebView can only be created/configured on the main thread, so this runs the setup
 * synchronously on it via the instrumentation.
 */
@RunWith(AndroidJUnit4::class)
class WebViewManagerTest {

    @Test
    fun setupWebView_hardensSecuritySettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var settings: WebSettings? = null

        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val webView = WebView(context)
            val manager = WebViewManager(context, webView)
            manager.setupWebView("about:blank", "order_1", "token_1", "sub_1")
            settings = webView.settings
        }

        val s = settings!!
        // WebSettings has no public getter for geolocation, so that flag isn't asserted here.
        assertFalse("allowFileAccess must be disabled", s.allowFileAccess)
        assertFalse("allowContentAccess must be disabled", s.allowContentAccess)
        assertFalse("database must be disabled", s.databaseEnabled)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, s.mixedContentMode)
        assertTrue("javaScriptEnabled must be on for checkout", s.javaScriptEnabled)
        assertTrue("domStorageEnabled must be on for checkout", s.domStorageEnabled)
    }
}
