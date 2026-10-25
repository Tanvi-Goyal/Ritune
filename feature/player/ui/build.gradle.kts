plugins {
    alias(libs.plugins.ritune.android.feature)
}

android {
    namespace = "com.ritune.feature.player.ui"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
