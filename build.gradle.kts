import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("android") version "1.9.0"
    id("maven-publish")
}

android {
    namespace = "tech.nimbbl.webviewsdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Core API SDK dependency
    //implementation(project(":nimbbl_mobile_kit_core_api_sdk"))
    implementation("com.github.nimbbl-tech:nimbbl_mobile_kit_core_api_sdk:3.0.15")
    
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
                
                // The sources jar is automatically included by the Android library component
                // No need to manually add it to avoid duplicates

                groupId = "com.github.nimbbl-tech"
                artifactId = "nimbbl-checkout-sdk"
                version = "3.0.11"

                pom {
                    name.set("nimbbl-checkout-sdk")
                    description.set("Nimbbl Checkout SDK for Android")
                }
            }
        }
    }
}
