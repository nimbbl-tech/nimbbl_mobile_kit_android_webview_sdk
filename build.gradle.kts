import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
//implementation("tech.nimbbl.sdk:nimbbl-checkout-core-sdk-java:3.0.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.auth0.android:jwtdecode:2.0.0")
    implementation("tech.nimbbl.sdk:nimbbl-mobile-kit-core-api-sdk:3.0.6+1") // example, adjust as per your release



}

publishing {
    val sdkArtifactId = "nimbbl-checkout-sdk"
    val sdkGroupId = "tech.nimbbl.sdk"
    val gitlabToken = "glpat-zswGgsyUM5yVbo9yy6RG"
    val sdkVersion = "3.0.5"
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
