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

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.7.10")
            version("compose", "1.2.0-alpha01-dev750")
            version("ktor", "2.1.3")
            version("i18nhelper", "0.0.5-SNAPSHOT")
            version("kotlinx.serialization", "1.3.3")
            alias("kotlin-logging").to("io.github.microutils:kotlin-logging:2.1.23")
        }
    }
}