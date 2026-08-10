plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "ai.blueview.weather"
    compileSdk = 34

    defaultConfig {
        applicationId  = "ai.blueview.weather"
        minSdk         = 26          // Android 8.0 — covers 95%+ of active devices
        targetSdk      = 34
        versionCode    = 10
        versionName    = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing. The keystore is never committed — CI materialises it from
    // the KEYSTORE_BASE64 secret and supplies the credentials via the environment,
    // so nothing secret lives in this file. A missing or empty keystore (local dev)
    // falls back to Gradle's default debug signing rather than failing the build.
    val keystoreFile  = rootProject.file("keystore/blueview-release.jks")
    val keystorePass  = System.getenv("KEYSTORE_PASSWORD")
    val keystoreAlias = System.getenv("KEY_ALIAS")
    val keyPass       = System.getenv("KEY_PASSWORD")
    val hasKeystore   = keystoreFile.exists() && keystoreFile.length() > 0L &&
                        !keystorePass.isNullOrBlank() &&
                        !keystoreAlias.isNullOrBlank() &&
                        !keyPass.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile     = keystoreFile
                storePassword = keystorePass
                keyAlias      = keystoreAlias
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Sign debug with the release key too: the APK published to GitHub Releases
        // is the debug one, and a stable key is what lets each version install over
        // the last instead of failing with "App not installed".
        debug {
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.webkit)      // WebViewAssetLoader — serves bundled Leaflet over https
    implementation(libs.activity.compose)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation + Lifecycle
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Serialization + Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)
}
