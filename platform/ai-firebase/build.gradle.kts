// Firebase AI Logic adapter for the domain's CoachGateway port. The only module that knows Firebase,
// so moving the coach to another provider means replacing this module and nothing else.
plugins {
    alias(libs.plugins.ritune.android.library)
    alias(libs.plugins.ritune.hilt)
}

android {
    namespace = "com.ritune.platform.ai.firebase"

    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":platform:android")) // @Dispatcher qualifiers

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
}
