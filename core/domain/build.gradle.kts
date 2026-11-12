plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ritune.core.domain"
}

dependencies {
    api(project(":core:models"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
