pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ritune"
include(":app")
include(":core")
include(":core:data")
include(":core:domain")
include(":core:models")
include(":core:database")
include(":feature")
include(":core:designsystem")
include(":feature:today")
include(":feature:today:ui")
include(":feature:rhythm")
include(":feature:routine")
include(":feature:onboarding")
include(":feature:player")
include(":feature:player:ui")
include(":feature:onboarding:ui")
include(":feature:rhythm:ui")
include(":feature:routine:ui")
include(":platform")
include(":platform:android")
include(":core:navigation")
