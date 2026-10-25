plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.android.compose)
}

android {
    namespace = "com.ritune.core.designsystem"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.tooling.preview)

    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
