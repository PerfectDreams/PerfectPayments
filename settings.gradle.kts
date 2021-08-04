pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "PerfectPayments"

includeBuild("locale-gen")
include(":common")
include(":backend")
include(":frontend")