plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("TranslationKeysGenerator")
}

repositories {
    mavenCentral()
}

translationKeysSettings {
    generatedPackage.set("net.perfectdreams.perfectpayments.i18n")
    languageSourceFolder.set("../resources/languages/en/")
}

configure<net.perfectdreams.i18nwrapper.TranslationKeysGenPluginExtension> {
    generatedPackage.set("net.perfectdreams.perfectpayments.i18n")
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
            kotlin.srcDir("build/generated/locales")
            resources.srcDir("../locales/")

            dependencies {
                api(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
                api("io.github.microutils:kotlin-logging:2.0.10")
            }
        }
    }
}

tasks {
    // HACKY WORKAROUND!!!
    // This makes the generateTranslationKeys task to always be ran after the compileKotlin step
    // We need to do this (instead of using withType) because, for some reason, it doesn't work and the task isn't executed.
    project.tasks.filter { it.name.startsWith("compileKotlin") }.forEach {
        it.dependsOn("generateTranslationKeys")
    }
}