import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

// Load .env into project properties (so findProperty works). .env is gitignored.
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.reader().use { reader ->
        reader.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val idx = trimmed.indexOf('=')
                if (idx > 0) {
                    val key = trimmed.substring(0, idx).trim()
                    val value = trimmed.substring(idx + 1).trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    if (!project.hasProperty(key)) {
                        rootProject.extensions.extraProperties.set(key, value)
                    }
                }
            }
        }
    }
}

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
    id("signing")
}

fun useLocalCoreSdk(): Boolean {
    val prop = findProperty("USE_LOCAL_CORE_SDK") as String?
    val env = System.getenv("USE_LOCAL_CORE_SDK")
    return when {
        prop != null -> prop.equals("true", ignoreCase = true)
        env != null -> env.equals("true", ignoreCase = true)
        else -> true
    }
}

// Dependency versions - aligned with Core API SDK (from version.properties when present)
val coreApiSdkVersion = versionProperties.getProperty("CORE_API_SDK_VERSION", "4.0.13")
val coreKtxVersion = "1.12.0"
val appCompatVersion = "1.6.1"
val materialVersion = "1.12.0"
val coroutinesVersion = "1.7.3"
val junitVersion = "4.13.2"
val junitExtVersion = "1.1.5"
val espressoVersion = "3.5.1"

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
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = true
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
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.appcompat:appcompat:$appCompatVersion")
    implementation("com.google.android.material:material:$materialVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")

    if (useLocalCoreSdk()) {
        implementation(project(":nimbbl_mobile_kit_core_api_sdk"))
    } else {
        implementation("tech.nimbbl:core-api-sdk:$coreApiSdkVersion")
    }
}

// Maven Central coordinates (tech.nimbbl namespace)
val PUBLISH_GROUP_ID = "tech.nimbbl"
val PUBLISH_ARTIFACT_ID_WEBVIEW = "webview-sdk"
val PUBLISH_VERSION = versionProperties.getProperty("SDK_VERSION", "1.0.0")

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = PUBLISH_GROUP_ID
                artifactId = PUBLISH_ARTIFACT_ID_WEBVIEW
                version = PUBLISH_VERSION

                pom {
                    name.set("Nimbbl WebView Checkout SDK")
                    description.set("Nimbbl Checkout WebView SDK for Android - in-app checkout experience.")
                    url.set("https://github.com/nimbbl-tech/nimbbl_mobile_kit_android_webview_sdk")
                    packaging = "aar"

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
        repositories {
            maven {
                name = "sonatype"
                val isSnapshot = PUBLISH_VERSION.endsWith("-SNAPSHOT")
                url = uri(
                    if (isSnapshot) {
                        "https://central.sonatype.com/repository/maven-snapshots/"
                    } else {
                        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                    }
                )
                credentials {
                    username = project.findProperty("centralPortalUsername") as String?
                        ?: System.getenv("CENTRAL_PORTAL_USERNAME")
                        ?: project.findProperty("ossrhTokenUsername") as String?
                        ?: System.getenv("OSSRH_TOKEN_USERNAME")
                        ?: ""
                    password = project.findProperty("centralPortalPassword") as String?
                        ?: System.getenv("CENTRAL_PORTAL_PASSWORD")
                        ?: project.findProperty("ossrhTokenSecret") as String?
                        ?: System.getenv("OSSRH_TOKEN_SECRET")
                        ?: ""
                }
            }
        }
    }
    signing {
        val signingKeyId: String? = project.findProperty("signing.keyId") as String?
        val signingPassword: String? = project.findProperty("signing.password") as String?
        val signingKey: String? = project.findProperty("signing.secretKeyRingFile") as String?
        if (signingKeyId != null && signingPassword != null && signingKey != null) {
            sign(publishing.publications["release"])
        }
    }
}
