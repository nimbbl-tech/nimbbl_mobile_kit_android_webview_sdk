pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
    plugins {
        id("com.android.library") version "8.10.1" apply false // Ensure consistency
        kotlin("android") version "1.9.0" apply false  // Ensure consistency
    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

// Include Core API SDK as a dependency
//include(":nimbbl_mobile_kit_core_api_sdk")
//project(":nimbbl_mobile_kit_core_api_sdk").projectDir = file("/Users/sandeepyadav/Documents/nimbbl_bitbucket_repos/nimbbl_mobile_kit_core_api_sdk")