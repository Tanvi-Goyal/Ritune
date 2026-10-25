plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
}

android {
    namespace = "com.ritune.core.database"
}

// Committed schema JSON per version; needed to write and test migrations once v1 ships.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:models"))

    api(libs.androidx.room3)
    ksp(libs.androidx.room.compiler)
}
