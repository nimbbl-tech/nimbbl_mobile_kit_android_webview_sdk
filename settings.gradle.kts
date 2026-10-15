pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version "8.11.1" apply false
        id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    }
}

fun useLocalCoreSdk(): Boolean {
    val prop = providers.gradleProperty("USE_LOCAL_CORE_SDK").orNull
    val env = System.getenv("USE_LOCAL_CORE_SDK")
    return when {
        prop != null -> prop.equals("true", ignoreCase = true)
        env != null -> env.equals("true", ignoreCase = true)
        else -> true
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nimbbl_mobile_kit_android_webview_sdk"

// Local development: build the KMP Core API SDK from the sibling repo. Gradle substitutes the
// `tech.nimbbl:core-api` dependency with its :core-api project, so nothing is published.
if (useLocalCoreSdk()) {
    includeBuild("../nimbbl_mobile_kit_kmp_core_api_sdk")
}
