package com.ritune.buildlogic

import com.android.build.api.dsl.CommonExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

const val COVERAGE_VARIANT = "coverage"
private const val TESTING_MODULE = ":core:testing"

internal fun Project.configureUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("kotlin-test"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
    }
}

internal fun Project.configureAndroidTestingModule() {
    if (path == TESTING_MODULE) return
    dependencies {
        add("testImplementation", project(TESTING_MODULE))
        add("androidTestImplementation", project(TESTING_MODULE))
    }
}

internal fun Project.configureInstrumentedTestDependencies() {
    dependencies {
        add("androidTestImplementation", libs.library("androidx-junit"))
        add("androidTestImplementation", libs.library("androidx-runner"))
        add("androidTestImplementation", libs.library("androidx-test-rules"))
        add("androidTestImplementation", libs.library("kotlin-test"))
        add("androidTestImplementation", libs.library("kotlinx-coroutines-test"))
        add("androidTestImplementation", libs.library("turbine"))
    }
}

internal fun Project.configureRobolectric() {
    extensions.configure<CommonExtension> {
        testOptions.unitTests.isIncludeAndroidResources = true
    }
    tasks.withType<Test>().configureEach {
        // Android resources add a generated test R class, which Gradle mistakes for test sources.
        failOnNoDiscoveredTests.set(false)
        // Robolectric reads raw FileDescriptor fields through this JDK-internal package.
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    }
    dependencies {
        add("testImplementation", libs.library("androidx-junit"))
        add("testImplementation", libs.library("robolectric"))
    }
}

internal fun Project.configureKover(sourceVariant: String) {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    extensions.configure<KoverProjectExtension> {
        currentProject {
            createVariant(COVERAGE_VARIANT) {
                add(sourceVariant)
            }
        }
    }
}
