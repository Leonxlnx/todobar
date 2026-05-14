import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.todobar.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.todobar.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "0.1.14"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        // Release builds are signed with a keystore generated in CI so that the
        // produced APK is installable from a GitHub release without further
        // ceremony. The keystore credentials are injected via env vars.
        create("release") {
            val keystorePath = System.getenv("TODOBAR_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("TODOBAR_KEYSTORE_PASSWORD") ?: "todobar"
                keyAlias = System.getenv("TODOBAR_KEY_ALIAS") ?: "todobar"
                keyPassword = System.getenv("TODOBAR_KEY_PASSWORD") ?: "todobar"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val keystorePath = System.getenv("TODOBAR_KEYSTORE_PATH")
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fall back to the debug signing config so that local
                // `assembleRelease` runs still produce an installable APK.
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")

    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
