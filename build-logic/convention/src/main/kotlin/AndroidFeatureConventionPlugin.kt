import com.ritune.buildlogic.libs
import com.ritune.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ritune.android.library")
        pluginManager.apply("ritune.android.compose")
        pluginManager.apply("ritune.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))

            add("implementation", platform(libs.library("androidx-compose-bom")))
            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("implementation", libs.library("androidx-compose-material3"))

            add("implementation", libs.library("androidx-appcompat"))
            add("implementation", libs.library("androidx-core-ktx"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))
        }
    }
}
