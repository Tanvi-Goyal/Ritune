plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
}

android {
    namespace = "com.ritune.core.data"
}

dependencies {
    api(project(":core:models"))
    api(project(":core:database"))
    api(project(":core:datastore"))
    implementation(project(":core:domain"))
}
