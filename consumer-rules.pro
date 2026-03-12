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
# JWT Token Parsing (Critical)
# ===========================================
# Auth0 JWT library (used by Core API SDK)
-keep class com.auth0.android.jwt.** { *; }
-keep class com.auth0.jwt.** { *; }

# JWT parsing methods
-keep class tech.nimbbl.coreapisdk.utils.extensions.NimbblSDKExtensions {
    public static java.lang.String parseJwtToken(android.content.Context, java.lang.String);
    public static java.lang.String getMerchantIDFromJwtToken(android.content.Context, java.lang.String);
}

# ===========================================
# Nimbbl SDK Classes (Essential)
# ===========================================
# Keep public SDK classes
-keep public class tech.nimbbl.webviewsdk.core.NimbblCheckoutSDK { *; }
-keep public class tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions { *; }
-keep public class tech.nimbbl.webviewsdk.models.interfaces.NimbblCheckoutPaymentListener { *; }

# Keep Core API SDK classes that WebView SDK depends on (ApiResult kept by Core API SDK consumer-rules.pro)
-keep class tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK { *; }
-keep class tech.nimbbl.coreapisdk.api.RestApiUtils { *; }

# ===========================================
# Model Classes (Essential)
# ===========================================
# Keep model classes used by org.json
-keep class tech.nimbbl.webviewsdk.models.** {
    <fields>;
    <init>(...);
    <methods>;
}

-keep class tech.nimbbl.coreapisdk.api.models.** {
    <fields>;
    <init>(...);
    <methods>;
}

-keep class tech.nimbbl.coreapisdk.data.models.** {
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