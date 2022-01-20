// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("net.perfectdreams.i18nhelper.plugin") version libs.versions.i18nhelper.get()
}

repositories {
    mavenCentral()
    maven("https://repo.perfectdreams.net/")
}

i18nHelper {
    generatedPackage.set("net.perfectdreams.perfectpayments.i18n")
    languageSourceFolder.set("../resources/languages/en/")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
    }

    js(IR) {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/languages")
            dependencies {
                api(kotlin("stdlib-common"))
                implementation("net.perfectdreams.i18nhelper:core:${libs.versions.i18nhelper.get()}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${libs.versions.kotlinx.serialization.get()}")
                api(libs.kotlin.logging)
            }
        }
    }
}