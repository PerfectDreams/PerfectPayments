plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.perfectdreams.net/")
    }
}