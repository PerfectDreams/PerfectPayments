pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://repo.perfectdreams.net/")
    }
}

rootProject.name = "PerfectPayments"

// includeBuild("locale-gen")
include(":common")
include(":backend")
include(":frontend")

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.6.10")
            version("compose", "1.0.1")
            version("ktor", "1.6.7")
            version("i18nhelper", "0.0.3-SNAPSHOT")
            version("kotlinx.serialization", "1.3.2")
            alias("kotlin-logging").to("io.github.microutils:kotlin-logging:2.1.21")
        }
    }
}