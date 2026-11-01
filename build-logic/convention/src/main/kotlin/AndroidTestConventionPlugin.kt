import com.android.build.api.dsl.TestExtension
import com.ritune.buildlogic.RituneSdk
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")

        extensions.configure<TestExtension> {
            compileSdk {
                version = release(RituneSdk.COMPILE)
            }
            defaultConfig.minSdk = RituneSdk.MIN
            defaultConfig.targetSdk = RituneSdk.TARGET
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            compileOptions.sourceCompatibility = JavaVersion.VERSION_11
            compileOptions.targetCompatibility = JavaVersion.VERSION_11
        }
    }
}
