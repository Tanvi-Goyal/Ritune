import com.android.build.api.dsl.LibraryExtension
import com.ritune.buildlogic.configureAndroid
import com.ritune.buildlogic.configureInstrumentedTestDependencies
import com.ritune.buildlogic.configureKover
import com.ritune.buildlogic.configureUnitTestDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroid()
        }
        configureUnitTestDependencies()
        configureInstrumentedTestDependencies()
        configureKover("debug")
    }
}
