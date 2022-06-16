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

val generateI18nKeys = tasks.register<net.perfectdreams.i18nhelper.plugin.GenerateI18nKeysTask>("generateI18nKeys") {
    generatedPackage.set("net.perfectdreams.perfectpayments.i18n")
    languageSourceFolder.set(file("../resources/languages/en/"))
    languageTargetFolder.set(file("$buildDir/generated/languages"))
    // TODO: You need to keep this because without it, Gradle complains that it is not set
    // This needs to be fixed in i18nHelper
    translationLoadTransform.set { file, map -> map }
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
            // If a task only has one output, you can reference the task itself
            kotlin.srcDir(generateI18nKeys)

            dependencies {
                api(kotlin("stdlib-common"))
                implementation("net.perfectdreams.i18nhelper:core:${libs.versions.i18nhelper.get()}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${libs.versions.kotlinx.serialization.get()}")
                api(libs.kotlin.logging)
            }
        }
    }
}