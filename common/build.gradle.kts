plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("net.perfectdreams.i18nhelper.plugin") version "0.0.1-SNAPSHOT"
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
                implementation("net.perfectdreams.i18nhelper:core:0.0.1-SNAPSHOT")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
                api("io.github.microutils:kotlin-logging:2.0.10")
            }
        }
    }
}