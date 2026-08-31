plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---------------------------------------------------------------------------
// Version
// ---------------------------------------------------------------------------

val appVersionName = "1.2.2"

/**
 * Derives versionCode from versionName: "1.2.0" -> 10200.
 *
 * Play permanently forbids reusing a versionCode, and a hand-maintained integer is
 * exactly how one gets reused or bumped backwards. major * 10_000 + minor * 100 +
 * patch is strictly monotonic for any forward version bump and allows 99 minors and
 * 99 patches per step. The last hand-set code was 12, so every derived value from
 * 0.0.13 upward is safely higher — 1.2.0 yields 10200.
 */
fun versionCodeOf(versionName: String): Int {
    val parts = versionName.split(".")
    require(parts.size == 3) { "versionName '$versionName' must be major.minor.patch" }
    val (major, minor, patch) = parts.map { part ->
        // tolerate a "-rc1"-style suffix on the patch segment
        part.takeWhile(Char::isDigit).toIntOrNull()
            ?: error("versionName '$versionName' has a non-numeric segment '$part'")
    }
    require(minor in 0..99 && patch in 0..99) {
        "versionName '$versionName': minor and patch must each be < 100 to stay monotonic"
    }
    return major * 10_000 + minor * 100 + patch
}

// ---------------------------------------------------------------------------
// Release signing inputs
// ---------------------------------------------------------------------------
// The keystore is never committed — CI materialises it from the KEYSTORE_BASE64
// secret and supplies the credentials via the environment, so nothing secret lives
// in this file. Read through Gradle's provider API rather than System.getenv() so
// the configuration cache records them as inputs instead of baking in stale values.

val keystoreFile  = rootProject.file("keystore/blueview-release.jks")
val keystorePass  = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val keystoreAlias = providers.environmentVariable("KEY_ALIAS").orNull
val keyPass       = providers.environmentVariable("KEY_PASSWORD").orNull

// What the release signing config still needs. Computed once at configuration time
// and only *consumed* by the verifyReleaseSigning task, so configuring the project
// (IDE sync, ./gradlew tasks) and building debug still work without a keystore.
val missingReleaseSigning: List<String> = buildList {
    if (!keystoreFile.exists() || keystoreFile.length() == 0L) {
        add("keystore file ${keystoreFile.path} (CI writes it from the KEYSTORE_BASE64 secret)")
    }
    if (keystorePass.isNullOrBlank())  add("KEYSTORE_PASSWORD (environment variable)")
    if (keystoreAlias.isNullOrBlank()) add("KEY_ALIAS (environment variable)")
    if (keyPass.isNullOrBlank())       add("KEY_PASSWORD (environment variable)")
}
val hasKeystore = missingReleaseSigning.isEmpty()

android {
    namespace  = "ai.blueview.weather"
    compileSdk = 36

    defaultConfig {
        applicationId  = "ai.blueview.weather"
        minSdk         = 26          // Android 8.0 — covers 95%+ of active devices
        targetSdk      = 36
        versionName    = appVersionName
        versionCode    = versionCodeOf(appVersionName)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Two distribution channels, and they are NOT interchangeable. Play's Device and
    // Network Abuse policy forbids an app it distributes from updating itself outside
    // Play or pointing users at another download of the same app, so the in-app update
    // checker exists only in the github flavour. The flag gates the UI; R8 then strips
    // the dead branch and its strings from the Play artifact, and the play source set
    // supplies a no-op UpdateChecker so no GitHub URL is compiled in at all.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
        }
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "true")
        }
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile     = keystoreFile
                storePassword = keystorePass
                keyAlias      = keystoreAlias
                keyPassword   = keyPass
            }
            // When the keystore is absent this config is left empty on purpose and is
            // never attached to the release build type; verifyReleaseSigning is what
            // stops a release from being built unsigned.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
            // Play warns that the bundle has native code without debug symbols. That
            // warning is currently unfixable and benign: the only .so files are prebuilts
            // from AndroidX (datastore_shared_counter, androidx.graphics.path) and they
            // ship already stripped — `file` reports "stripped", 0 debug sections — so
            // there is nothing for AGP to extract. This setting is therefore a no-op
            // today; it is kept so symbols are collected automatically if this app ever
            // gains native code of its own. R8's proguard.map IS included automatically,
            // so Kotlin/Java stack traces in Play Console are already deobfuscated.
            ndk { debugSymbolLevel = "FULL" }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Sign debug with the release key when it is available: the APK published to
        // GitHub Releases is the debug one, and a stable key is what lets each version
        // install over the last instead of failing with "App not installed". Without a
        // keystore, debug falls back to Gradle's default debug signing — which is fine
        // for local work and is why only *release* builds are hard-failed below.
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

// A release build with an incomplete signing config used to silently produce an
// UNSIGNED artifact, which Play rejects only at upload time — after CI has gone
// green. Fail at build time instead, naming exactly what is missing.
//
// This is a task rather than a configuration-time error so that merely configuring
// the project without a keystore still works. The list it checks was captured above
// at configuration time, so the task action touches no environment variables and
// stays configuration-cache compatible.
// Formatted at configuration time into a plain String. The task action must not close
// over the List: buildList returns a kotlin ListBuilder, and the configuration cache
// fails to deserialize it ("Could not load the value of field `collection`"), which
// masks this check's real message behind an internal Gradle error.
val releaseSigningError: String? =
    if (missingReleaseSigning.isEmpty()) null
    else buildString {
        appendLine("Refusing to build a release artifact: signing is not configured.")
        appendLine("An unsigned release AAB/APK is rejected by Google Play.")
        appendLine("Missing:")
        missingReleaseSigning.forEach { appendLine("  - $it") }
        append("Provide these and re-run. Debug builds do not need them.")
    }

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group       = "verification"
    description = "Fails the build if the release signing keystore or credentials are missing."
    val error = releaseSigningError
    doLast {
        if (error != null) throw GradleException(error)
    }
}

// The flavours mean there is no plain assembleRelease/bundleRelease any more; the
// real names are assemble{Play,Github}Release and bundle{Play,Github}Release. Match
// on the pattern so any future flavour is covered automatically.
tasks.matching { task ->
    (task.name.startsWith("assemble") || task.name.startsWith("bundle")) &&
        task.name.endsWith("Release")
}.configureEach { dependsOn(verifyReleaseSigning) }

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
