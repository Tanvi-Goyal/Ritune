import com.android.build.api.dsl.CommonExtension
import com.ritune.buildlogic.libs
import com.ritune.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("io.github.takahirom.roborazzi")

        extensions.configure<CommonExtension> {
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        // Android resources add a generated test R class, which Gradle mistakes for test sources.
        tasks.withType<Test>().configureEach {
            failOnNoDiscoveredTests.set(false)
        }

        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("testImplementation", bom)
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-junit-rule"))

            add("androidTestImplementation", bom)
            add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))

            add("debugImplementation", bom)
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
