plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib") version "3.1.2"
}

group = "net.perfectdreams.perfectpayments"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.perfectdreams.net/")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":common"))

    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.0.1")

    // Database
    implementation("org.postgresql:postgresql:42.2.18")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.jetbrains.exposed:exposed-core:0.32.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.32.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.32.1")

    // ICU4J
    implementation("com.ibm.icu:icu4j:69.1")

    // Used for Locales
    implementation("org.yaml:snakeyaml:1.29")
    implementation("com.charleskorn.kaml:kaml:0.35.0")
    implementation("net.perfectdreams.i18nhelper:core:0.0.1-SNAPSHOT")
    implementation("net.perfectdreams.i18nhelper.formatters:icu-messageformat-jvm:0.0.2-SNAPSHOT")

    implementation("com.github.ben-manes.caffeine:caffeine:2.8.8")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.stripe:stripe-java:20.3.0")
    implementation("com.paypal.sdk:checkout-sdk:1.0.2")
    implementation("club.minnced:discord-webhooks:0.5.4")

    implementation("io.github.microutils:kotlin-logging:2.0.4")

    // Sequins
    api("net.perfectdreams.sequins.ktor:base-route:1.0.2")

    // Ktor
    implementation("io.ktor:ktor-client-core:${Versions.KTOR}")

    // We use "Apache" because "CIO", for some reason, has issues with PayPal's API
    implementation("io.ktor:ktor-client-apache:${Versions.KTOR}")
    implementation("io.ktor:ktor-server-core:${Versions.KTOR}")
    implementation("io.ktor:ktor-server-netty:${Versions.KTOR}")
}

jib {
    to {
        image = "ghcr.io/perfectdreams/perfectpayments-backend"

        auth {
            username = System.getProperty("DOCKER_USERNAME") ?: System.getenv("DOCKER_USERNAME")
            password = System.getProperty("DOCKER_PASSWORD") ?: System.getenv("DOCKER_PASSWORD")
        }
    }

    from {
        image = "openjdk:17-slim-buster"
    }
}

val jsBrowserProductionWebpack = tasks.getByPath(":frontend:jsBrowserProductionWebpack") as org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

tasks {
    val sass = sassTask("style.scss")

    processResources {
        // We need to wait until the JS build finishes and the SASS files are generated
        dependsOn(jsBrowserProductionWebpack)
        dependsOn(sass)

        from("../resources/") // Include folders from the resources root folder

        // Copy the output from the frontend task to the backend resources
        from(jsBrowserProductionWebpack.destinationDirectory) {
            into("static/assets/js/")
        }

        // Same thing with the SASS output
        from(File(buildDir, "sass")) {
            into("static/assets/css/")
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
