// Android adapters for the domain's ports: the foreground service that keeps a routine running, the
// PlayerController the UI talks to, cue channels (haptics, speech, sound), the notification, DataStore
// settings, and the system clock. Depends on the domain; never on features or :core:data.
// Hilt aliases follow common catalog names; rename them to match your libs.versions.toml.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ritune.platform.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:domain")) // model, player, and ports come with it
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)               // NotificationCompat, ServiceCompat, ContextCompat
    implementation(libs.kotlinx.coroutines.android)      // Dispatchers.Main
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}