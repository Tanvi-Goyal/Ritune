plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover)
}

dependencies {
    subprojects
        .filter { it.buildFile.exists() && it.path !in setOf(":core:testing", ":core:screenshot-testing") }
        .forEach { kover(it) }
}

kover {
    currentProject {
        createVariant("coverage") {}
    }
    reports {
        filters {
            excludes {
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                    "dagger.hilt.InstallIn",
                )
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "Hilt_*",
                    "*.Hilt_*",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.internal.*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                )
            }
        }
    }
}
