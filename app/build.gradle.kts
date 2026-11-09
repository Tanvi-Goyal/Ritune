plugins {
    alias(libs.plugins.ritune.android.application)
    alias(libs.plugins.ritune.android.compose)
    alias(libs.plugins.ritune.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ritune"
    val versionNameProp = providers.gradleProperty("versionName").orNull

    fun versionCodeOf(name: String): Int {
        val parts = name.split(".").map { it.toIntOrNull() }
        require(parts.size == 3 && parts.all { it != null && it in 0..99 }) {
            "versionName must be MAJOR.MINOR.PATCH with each part 0-99, was '$name'"
        }
        val (major, minor, patch) = parts.map { it!! }
        return major * 10000 + minor * 100 + patch
    }

    defaultConfig {
        applicationId = "com.ritune"
        versionName = versionNameProp ?: "1.0-dev"
        versionCode = versionNameProp?.let(::versionCodeOf) ?: 1
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable(
                "RITUNE_KEYSTORE_PATH"
            ).orNull
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = providers.environmentVariable(
                    "RITUNE_KEYSTORE_PASSWORD"
                ).get()
                keyAlias = providers.environmentVariable(
                    "RITUNE_KEY_ALIAS"
                ).get()
                keyPassword = providers.environmentVariable(
                    "RITUNE_KEY_PASSWORD"
                ).get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }

        create("benchmark") {
            initWith(getByName("release"))          // same R8 and non-debuggable setup as release
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks += listOf("release")  // libraries without a benchmark variant use their release one
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkDependencies = true
    }

    // The ViewModels emit androidx.tracing sections, which call android.os.Trace and
    // android.util.Log. JVM tests have no Android, so let those calls return defaults.
    testOptions.unitTests.isReturnDefaultValues = true
}

// The baselineprofile plugin copies release into nonMinifiedRelease and turns off the old
// isMinifyEnabled, but not `optimization`, so R8 still renamed classes and the generated rules
// named obfuscated classes. Rules must use real names; the release build maps them through R8.
// finalizeDsl runs after the plugin has set the build type up.
androidComponents {
    finalizeDsl { android ->
        android.buildTypes.findByName("nonMinifiedRelease")?.optimization?.enable = false
    }
    // The plugin only adds the generated profile to the build types it manages. The Macrobenchmarks
    // measure `benchmark`, so it must ship the same profile as release.
    onVariants(selector().withBuildType("benchmark")) { variant ->
        variant.sources.baselineProfiles?.addStaticSourceDirectory("src/main/generated/baselineProfiles")
    }
}

baselineProfile {
    // In src/main rather than src/release, so every build type ships it, including the
    // benchmark build type the Macrobenchmarks measure.
    mergeIntoMain = true
    // Uses the startup rules to put startup classes first in the primary dex.
    dexLayoutOptimization = true
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":platform:android"))

    implementation(project(":feature:onboarding:ui"))
    implementation(project(":feature:rhythm:ui"))
    implementation(project(":feature:routine:ui"))
    implementation(project(":feature:today:ui"))
    implementation(project(":feature:player:ui"))
    implementation(project(":feature:settings:ui"))

    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    // Compiles the bundled profile at install on devices where Play doesn't.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.androidx.navigation.testing)

    androidTestImplementation(project(":core:navigation"))
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // AI lab, debug builds only until the coach ships, so release stays offline.
    debugImplementation(platform(libs.firebase.bom))
    debugImplementation(libs.firebase.ai)
    debugImplementation(libs.firebase.appcheck.debug)
}
