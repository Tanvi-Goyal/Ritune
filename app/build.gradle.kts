plugins {
    alias(libs.plugins.ritune.android.application)
    alias(libs.plugins.ritune.android.compose)
    alias(libs.plugins.ritune.hilt)
    alias(libs.plugins.kotlin.serialization)
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
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkDependencies = true
    }
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
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.uiautomator)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
