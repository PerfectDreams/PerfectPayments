pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://repo.perfectdreams.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "PerfectPayments"

// includeBuild("locale-gen")
include(":common")
include(":backend")
include(":frontend")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.0")
            version("compose", "1.6.11")
            version("ktor", "2.1.3")
            version("i18nhelper", "0.0.5-SNAPSHOT")
            version("kotlinx.serialization", "1.3.3")
        }
    }
}