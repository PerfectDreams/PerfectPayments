plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
}

group = "net.perfectdreams.perfectpayments"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-dev/")
    maven("https://dl.bintray.com/kotlin/kotlin-eap/")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlinx.html")
    maven("https://jcenter.bintray.com")
    maven("https://repo.perfectdreams.net/")
    maven("https://dl.bintray.com/kotlin/ktor/")
    maven("https://repo.perfectdreams.net")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.0.1")

    // Database
    implementation("org.postgresql:postgresql:42.2.18")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.jetbrains.exposed:exposed-core:0.28.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.28.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.28.1")

    implementation("com.github.ben-manes.caffeine:caffeine:2.8.8")
    implementation("com.typesafe:config:1.4.1")
    implementation("org.yaml:snakeyaml:1.27")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.stripe:stripe-java:20.3.0")
    implementation("com.paypal.sdk:checkout-sdk:1.0.2")
    implementation("club.minnced:discord-webhooks:0.5.4")

    implementation("io.github.microutils:kotlin-logging:2.0.4")

    // Ktor
    implementation("io.ktor:ktor-client-core:1.4.3")
    // We use "Apache" because "CIO", for some reason, has issues with PayPal's API
    implementation("io.ktor:ktor-client-apache:1.4.3")
    implementation("io.ktor:ktor-server-core:1.4.3")
    implementation("io.ktor:ktor-server-netty:1.4.3")
}

tasks {
    val fatJar = task("fatJar", type = Jar::class) {
        println("Building fat jar for ${project.name}...")

        archiveBaseName.set("${project.name}-fat")

        manifest {
            attributes["Main-Class"] = "net.perfectdreams.perfectpayments.PerfectPaymentsLauncher"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ", transform = { "libs/" + it.name })
        }

        val libs = File(rootProject.projectDir, "libs")
        // libs.deleteRecursively()
        libs.mkdirs()

        from(configurations.runtimeClasspath.get().mapNotNull {
            val output = File(libs, it.name)

            if (!output.exists())
                it.copyTo(output, true)

            null
        })

        with(jar.get() as CopySpec)
    }

    "build" {
        dependsOn(fatJar)
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