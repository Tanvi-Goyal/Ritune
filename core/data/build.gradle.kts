plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "com.ritune.core.data"
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
    api(project(":core:database"))
    implementation(project(":core:domain"))

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
