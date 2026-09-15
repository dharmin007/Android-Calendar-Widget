pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // NOTE: mavenCentral() intentionally omitted (unreachable from this build env).
    repositories {
        google()
    }
}
rootProject.name = "Android-Calendar-Widget"
include(":calendarwidget")
include(":sample")
