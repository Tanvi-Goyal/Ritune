plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "com.ritune.core.domain"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core:models"))
    api(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
