package com.ritune.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

object RituneSdk {
    const val COMPILE = 37
    const val MIN = 26
    const val TARGET = 37
}

const val MANAGED_DEVICE = "pixel6Api34"

internal fun CommonExtension.configureAndroid(hasDeviceTests: Boolean) {
    compileSdk {
        version = release(RituneSdk.COMPILE)
    }
    defaultConfig.minSdk = RituneSdk.MIN
    defaultConfig.testInstrumentationRunner = "com.ritune.core.testing.HiltTestRunner"
    compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    compileOptions.targetCompatibility = JavaVersion.VERSION_11

    if (!hasDeviceTests) return
    testOptions.managedDevices.localDevices.create(MANAGED_DEVICE) {
        device = "Pixel 6"
        sdkVersion = 34
        systemImageSource = "aosp-atd"
    }
}
