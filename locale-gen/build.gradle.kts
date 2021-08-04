plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.9.0")
    implementation("org.yaml:snakeyaml:1.29")
    implementation("com.ibm.icu:icu4j:69.1")
}

gradlePlugin {
    plugins.register("LocaleGenerator") {
        id = "TranslationKeysGenerator"
        implementationClass = "net.perfectdreams.i18nwrapper.TranslationKeysGenPlugin"
    }
}