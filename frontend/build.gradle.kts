// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version libs.versions.compose.get()
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
                implementation("io.ktor:ktor-client-js:${libs.versions.ktor.get()}")

                // Locales
                implementation(npm("intl-messageformat", "9.8.1"))
                implementation("net.perfectdreams.i18nhelper:core:${libs.versions.i18nhelper.get()}")
                implementation("net.perfectdreams.i18nhelper.formatters:intl-messageformat-js:${libs.versions.i18nhelper.get()}")
            }
        }
    }
}

// Reduces Kotlin/JS bundle size
// https://youtrack.jetbrains.com/issue/KTOR-1084
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xir-per-module", "-Xir-property-lazy-initialization")
    }
}