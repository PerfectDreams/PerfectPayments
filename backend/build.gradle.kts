// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
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

    implementation("ch.qos.logback:logback-classic:1.3.0-alpha12")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${libs.versions.kotlinx.serialization.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.serialization.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${libs.versions.kotlinx.serialization.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${libs.versions.kotlinx.serialization.get()}")

    // Database
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.37.3")

    // ICU4J
    implementation("com.ibm.icu:icu4j:70.1")

    // Used for Locales
    implementation("org.yaml:snakeyaml:1.29")
    implementation("com.charleskorn.kaml:kaml:0.38.0")
    implementation("net.perfectdreams.i18nhelper:core:${libs.versions.i18nhelper.get()}")
    implementation("net.perfectdreams.i18nhelper.formatters:icu-messageformat-jvm:${libs.versions.i18nhelper.get()}")

    implementation("com.github.ben-manes.caffeine:caffeine:3.0.5")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("com.stripe:stripe-java:20.3.0")
    implementation("com.paypal.sdk:checkout-sdk:1.0.5")
    implementation("club.minnced:discord-webhooks:0.7.5")

    implementation(libs.kotlin.logging)

    // Sequins
    api("net.perfectdreams.sequins.ktor:base-route:1.0.4")

    // Ktor
    implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")

    // We use "Apache" because "CIO", for some reason, has issues with PayPal's API
    implementation("io.ktor:ktor-client-apache:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-compression:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-caching-headers:${libs.versions.ktor.get()}")
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
    val sass = sassTask("style.scss", "style.css")

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
