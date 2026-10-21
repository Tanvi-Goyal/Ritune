plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ritune.core.database"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Committed schema JSON per version; needed to write and test migrations once v1 ships.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:models"))

    api(libs.androidx.room3)
    ksp(libs.androidx.room.compiler)

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
