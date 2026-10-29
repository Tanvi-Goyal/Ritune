import com.ritune.buildlogic.configureRobolectric
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidRobolectricConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureRobolectric()
}
