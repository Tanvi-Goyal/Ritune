plugins {
    alias(libs.plugins.ritune.android.library)
}

android {
    namespace = "com.ritune.core.testing"
}

dependencies {
    api(project(":core:domain"))
    api(libs.junit)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)

    implementation(libs.androidx.runner)
    implementation(libs.hilt.android.testing)
}
