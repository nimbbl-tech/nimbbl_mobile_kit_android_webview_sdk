pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version "8.2.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.0" apply false
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
// Local Core API SDK (uncomment for development):
// include(":nimbbl_mobile_kit_core_api_sdk")
// project(":nimbbl_mobile_kit_core_api_sdk").projectDir = file("../nimbbl_mobile_kit_core_api_sdk")
