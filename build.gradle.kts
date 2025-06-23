import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
}

android {
    namespace = "tech.nimbbl.webviewsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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

tasks.register<Wrapper>("nimbbl_webviewsdk_wrapper") {
    gradleVersion = "8.11.1"
}

tasks.register("nimbbl_webviewsdk_prepareKotlinBuildScriptModel") {}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.auth0.android:jwtdecode:2.0.0")
    implementation(project(":nimbbl_mobile_kit_core_api_sdk"))
   // implementation("com.github.nimbbl-tech:nimbbl_mobile_kit_core_api_sdk:3.0.7")
}

// Optional: include sources in the published artifact
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.nimbbl-tech"
                artifactId = "nimbbl-checkout-sdk"
                version = "3.0.6"

                pom {
                    name.set("nimbbl-checkout-sdk")
                    description.set("Nimbbl Checkout SDK for Android")
                }
            }
        }

        repositories {
            maven {
                name = "jitpack"
                url = uri("$buildDir/repo")
            }
        }
    }
}
