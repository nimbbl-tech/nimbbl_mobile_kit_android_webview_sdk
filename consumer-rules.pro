# ===========================================
# Nimbbl WebView SDK - Consumer ProGuard Rules
# These rules are automatically applied when merchants use minification
# ===========================================

# ===========================================
# WebView JavaScript Interface (Critical)
# ===========================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep specific JavaScript interface class used by SDK
-keep class tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity$INimmbleWebViewInterface { *; }

# ===========================================
# Nimbbl SDK Classes (Essential)
# ===========================================
# Keep public SDK classes
-keep public class tech.nimbbl.webviewsdk.core.NimbblCheckoutSDK { *; }
-keep public class tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions { *; }
-keep public class tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener { *; }

# Core API SDK classes (tech.nimbbl.coreapi.**) are kept by the KMP Core API SDK's own consumer-rules.pro

# ===========================================
# Model Classes (Essential)
# ===========================================
# Keep model classes used by org.json
-keep class tech.nimbbl.webviewsdk.models.** {
    <fields>;
    <init>(...);
    <methods>;
}

# ===========================================
# Essential Attributes
# ===========================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===========================================
# Suppress Warnings
# ===========================================
-dontwarn kotlin.reflect.**
-dontwarn kotlin.Unit
-dontwarn kotlin.jvm.internal.**