plugins {
    alias(libs.plugins.ritune.android.feature)
}

android {
    namespace = "com.ritune.feature.rhythm.ui"
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
