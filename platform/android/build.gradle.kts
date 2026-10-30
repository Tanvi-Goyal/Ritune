// Android adapters for the domain's ports: the foreground service that keeps a routine running, the
// PlayerController the UI talks to, cue channels (haptics, speech, sound), the notification, DataStore
// settings, and the system clock. Depends on the domain; never on features or :core:data.
plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.android.robolectric)
    alias(libs.plugins.ritune.hilt)
}

android {
    namespace = "com.ritune.platform.android"
}

dependencies {
    implementation(project(":core:domain")) // model, player, and ports come with it
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)               // NotificationCompat, ServiceCompat, ContextCompat
    implementation(libs.kotlinx.coroutines.android)      // Dispatchers.Main
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.tracing)
}
