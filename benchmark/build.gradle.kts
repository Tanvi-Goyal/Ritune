plugins {
    alias(libs.plugins.ritune.android.test)
}

android {
    namespace = "com.ritune.benchmark"
    targetProjectPath = ":app"

    // The test runs in its own process, so killing the app for a cold start doesn't kill the test.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Pairs with the app's benchmark build type. Only this test APK is debug-signed;
        // the app it measures keeps the release key.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
}
