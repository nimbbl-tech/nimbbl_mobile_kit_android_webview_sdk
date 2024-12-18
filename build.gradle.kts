import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
}


android {
    namespace = "tech.nimbbl.webviewsdk"
    compileSdk = 34

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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    gradleVersion = "8.10.2"
}
tasks.register("nimbbl_webviewsdk_prepareKotlinBuildScriptModel"){}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.kotlinx.coroutines.android)
    //implementation("tech.nimbbl.sdk:nimbbl-checkout-core-sdk-java:3.0.1")
    implementation(libs.nimbbl.checkout.core.sdk.java)
    //implementation (project(":nimbbl_mobile_kit_core_api_sdk"))
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation ("com.auth0.android:jwtdecode:2.0.0")


}

publishing {
    val sdkArtifactId = "nimbbl-checkout-sdk"
    val sdkGroupId = "tech.nimbbl.sdk"
    val gitlabToken = "glpat-zswGgsyUM5yVbo9yy6RG"
    val sdkVersion = "3.0.4"
    val mavenRepo = "https://gitlab.com/api/v4/projects/25847308/packages/maven"


    publications {
        create<MavenPublication>("aarArchive") {
            groupId = sdkGroupId
            artifactId = sdkArtifactId
            version = sdkVersion

            artifact("${project.buildDir}/outputs/aar/${project.name}-release.aar") {
                extension = "aar"
            }

            pom.withXml {
                val pomXml = asNode()
                pomXml.appendNode("name", sdkArtifactId)
                pomXml.appendNode("description", "Nimbbl Checkout SDK for Android")

                val dependenciesNode = pomXml.appendNode("dependencies")
                configurations.getByName("releaseCompileClasspath").resolvedConfiguration.firstLevelModuleDependencies.forEach {
                    val groupId = it.moduleGroup
                    val artifactId = it.moduleName

                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", groupId)
                    dependencyNode.appendNode("artifactId", artifactId)
                    dependencyNode.appendNode("version", it.moduleVersion)
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(mavenRepo)
            if (!mavenRepo.startsWith("file")) {
                credentials(HttpHeaderCredentials::class) {
                    name = "Private-Token"
                    value = gitlabToken
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}
