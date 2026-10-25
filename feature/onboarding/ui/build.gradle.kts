plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ritune.feature.onboarding.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
