plugins {
    alias(libs.plugins.ritune.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

val profileDevice = "pixel6Api34Aosp"

android {
    namespace = "com.ritune.baselineprofile"
    targetProjectPath = ":app"

    // A full AOSP image: the ATD image used for UI tests strips system apps and services,
    // so it can run code paths a real phone wouldn't.
    testOptions.managedDevices.localDevices.create(profileDevice) {
        device = "Pixel 6"
        sdkVersion = 34
        systemImageSource = "aosp"
    }
}

baselineProfile {
    managedDevices += profileDevice
    useConnectedDevices = false
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
}
