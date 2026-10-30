plugins {
    alias(libs.plugins.ritune.android.feature)
}

android {
    namespace = "com.ritune.feature.today.ui"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
