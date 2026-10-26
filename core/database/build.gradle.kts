plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.ritune.core.database"
}

// Committed schema JSON per version; migration tests read it from androidTest assets.
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:models"))

    api(libs.androidx.room3)
    ksp(libs.androidx.room.compiler)

    androidTestImplementation(libs.androidx.room3.testing)
}
