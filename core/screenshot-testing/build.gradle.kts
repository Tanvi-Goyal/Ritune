plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.android.compose)
}

android {
    namespace = "com.ritune.core.screenshottesting"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.roborazzi)
    api(libs.robolectric)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
}
