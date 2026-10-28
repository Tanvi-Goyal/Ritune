import com.ritune.buildlogic.configureRobolectric
import com.ritune.buildlogic.libs
import com.ritune.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

private const val SCREENSHOT_TESTING_MODULE = ":core:screenshot-testing"

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("io.github.takahirom.roborazzi")
        configureRobolectric()

        dependencies {
            if (path != SCREENSHOT_TESTING_MODULE) {
                add("testImplementation", project(SCREENSHOT_TESTING_MODULE))
            }
            val bom = platform(libs.library("androidx-compose-bom"))
            add("testImplementation", bom)
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
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
