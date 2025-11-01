import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

// Load version properties
val versionProperties = Properties()
val versionPropertiesFile = file("version.properties")
if (versionPropertiesFile.exists()) {
    versionProperties.load(versionPropertiesFile.inputStream())
}

plugins {
    id("com.android.library")
    kotlin("android") version "1.9.0"
    id("maven-publish")
}


android {
    namespace = "tech.nimbbl.webviewsdk"
    compileSdk = versionProperties.getProperty("COMPILE_ANDROID_SDK", "34").toInt()

    defaultConfig {
        minSdk = versionProperties.getProperty("MIN_ANDROID_SDK", "21").toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // Build config fields for version information
        buildConfigField("String", "SDK_VERSION", "\"${versionProperties.getProperty("SDK_VERSION", "1.0.0")}\"")
        buildConfigField("String", "BUILD_DATE", "\"${versionProperties.getProperty("BUILD_DATE", "unknown")}\"")
        buildConfigField("String", "BUILD_TYPE", "\"${versionProperties.getProperty("BUILD_TYPE", "debug")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Enable SDK obfuscation
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}


tasks.register("nimbbl_webviewsdk_prepareKotlinBuildScriptModel") {}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Core API SDK dependency - latest stable
    implementation(project(":nimbbl_mobile_kit_core_api_sdk"))
    //implementation("com.github.nimbbl-tech:nimbbl_mobile_kit_core_api_sdk:v4.0.8")
    //implementation("com.github.nimbbl-tech:nimbbl_mobile_kit_core_api_sdk:3.0.15")
    //implementation("org.bitbucket.nimbbl-tech:nimbbl_mobile_kit_core_api_sdk:v4.0.2")
    
    
    // JAXB dependencies are included via the core API SDK dependency
    // implementation("javax.xml.bind:jaxb-api:2.3.1")
    // implementation("org.glassfish.jaxb:jaxb-runtime:2.3.1")
    // implementation("javax.activation:activation:1.1.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                
                // JitPack coordinates (for backward compatibility)
                groupId = "com.github.nimbbl-tech"
                artifactId = "nimbbl-checkout-sdk"
                version = versionProperties.getProperty("SDK_VERSION", "1.0.0")

                pom {
                    name.set("nimbbl-checkout-sdk")
                    description.set("Nimbbl Checkout SDK for Android - Semantic Version ${versionProperties.getProperty("SDK_VERSION", "1.0.0")}")
                    url.set("https://github.com/nimbbl-tech/nimbbl_mobile_kit_android_webview_sdk")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("nimbbl-tech")
                            name.set("Bigital Technologies Pvt. Ltd")
                            email.set("tech@nimbbl.biz")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/nimbbl-tech/nimbbl_mobile_kit_android_webview_sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/nimbbl-tech/nimbbl_mobile_kit_android_webview_sdk.git")
                        url.set("https://github.com/nimbbl-tech/nimbbl_mobile_kit_android_webview_sdk")
                    }
                }
            }
        }
    }
}

// Core API SDK is now used as a JitPack dependency from GitHub
