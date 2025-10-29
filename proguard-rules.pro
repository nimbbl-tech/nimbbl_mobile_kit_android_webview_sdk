# ===========================================
# Nimbbl WebView SDK - Internal ProGuard Rules
# These rules are used when building the SDK itself
# ===========================================

# ===========================================
# Public API Protection (Keep Readable)
# ===========================================
# Keep all public classes and methods that merchants use
-keep public class tech.nimbbl.webviewsdk.core.NimbblCheckoutSDK { *; }
-keep public class tech.nimbbl.webviewsdk.models.NimbblCheckoutOptions { *; }
-keep public class tech.nimbbl.webviewsdk.models.NimbblCheckoutUserInfo { *; }
-keep public class tech.nimbbl.webviewsdk.core.NimbblShopOrderCreation { *; }
-keep public class tech.nimbbl.webviewsdk.models.CreateOrderResponse { *; }

# Keep public interfaces
-keep public interface tech.nimbbl.webviewsdk.models.interfaces.** { *; }

# Keep public methods in public classes
-keepclassmembers public class tech.nimbbl.webviewsdk.** {
    public <methods>;
}

# Keep companion objects
-keep class tech.nimbbl.webviewsdk.**$Companion { *; }

# Keep Kotlin extension functions
-keep class tech.nimbbl.webviewsdk.**Kt { *; }

# ===========================================
# WebView JavaScript Interface (Critical)
# ===========================================
# Keep JavaScript interface methods - these MUST remain readable
-keepclassmembers class tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity$INimmbleWebViewInterface {
    @android.webkit.JavascriptInterface public <methods>;
}

# Keep the JavaScript interface class itself
-keep class tech.nimbbl.webviewsdk.ui.activity.NimbblCheckoutMainActivity$INimmbleWebViewInterface { *; }

# ===========================================
# JSON Serialization (Critical)
# ===========================================
# Keep all model classes used for JSON serialization
-keep class tech.nimbbl.webviewsdk.models.** { *; }
-keep class tech.nimbbl.coreapisdk.api.models.** { *; }

# Keep JSON serialization annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===========================================
# Retrofit Services (Critical)
# ===========================================
# Keep Retrofit service interfaces
-keep interface tech.nimbbl.**.api.services.** { *; }

# Keep Retrofit annotations
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ===========================================
# Obfuscation Configuration
# ===========================================
# Important: Model classes MUST stay in original packages for Gson
# Don't repackage model classes - Gson needs exact class names and packages
-keep class tech.nimbbl.webviewsdk.models.** { *; }
-keep class tech.nimbbl.coreapisdk.api.models.** { *; }
-keep class tech.nimbbl.coreapisdk.data.models.** { *; }

# Repackage only non-model classes to unique package to avoid conflicts
-repackageclasses 'tech.nimbbl.webviewsdk.obfuscated'

# ===========================================
# Internal Classes (Can Be Obfuscated)
# ===========================================
# Allow obfuscation of internal implementation classes
# (These will be obfuscated but still functional)

# ===========================================
# Dependencies
# ===========================================
# Keep essential dependency classes
-keep class okhttp3.OkHttpClient { *; }
-keep class retrofit2.Retrofit { *; }
-keep class com.google.gson.Gson { *; }

# Disable string concatenation optimization (not available on Android)
-dontoptimize

# Suppress warnings
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn tech.nimbbl.coreapisdk.core.NimbblCoreApiSDK$Companion
-dontwarn tech.nimbbl.coreapisdk.utils.extensions.NimbblSDKExtensionsKt
-dontwarn tech.nimbbl.coreapisdk.utils.logging.EventLoggingUtils

# ===========================================
# Android Framework
# ===========================================
# Keep Android WebView classes
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# ===========================================
# Debugging Support
# ===========================================
# Keep line numbers for better error messages
-keepattributes SourceFile,LineNumberTable

# Keep generic signatures for reflection
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*