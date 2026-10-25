plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
}

android {
    namespace = "com.ritune.core.datastore"
}

dependencies {
    api(project(":core:models"))
    api(libs.androidx.datastore.preferences)
}
