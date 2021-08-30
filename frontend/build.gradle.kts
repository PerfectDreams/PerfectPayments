// Add compose gradle plugin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.0.0-alpha4-build321"
}

// Add maven repositories
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

// Enable JS(IR) target and add dependencies
kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation(project(":common"))
                implementation("io.ktor:ktor-client-js:1.6.2")

                // Locales
                implementation(npm("intl-messageformat", "9.8.1"))
                implementation("net.perfectdreams.i18nhelper:core:0.0.1-SNAPSHOT")
                implementation("net.perfectdreams.i18nhelper.formatters:intl-messageformat-js:0.0.2-SNAPSHOT")
            }
        }
    }
}