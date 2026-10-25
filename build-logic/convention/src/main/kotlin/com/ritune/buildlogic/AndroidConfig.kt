package com.ritune.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

object RituneSdk {
    const val COMPILE = 37
    const val MIN = 26
    const val TARGET = 37
}

internal fun CommonExtension.configureAndroid() {
    compileSdk {
        version = release(RituneSdk.COMPILE)
    }
    defaultConfig.minSdk = RituneSdk.MIN
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    compileOptions.targetCompatibility = JavaVersion.VERSION_11
}
