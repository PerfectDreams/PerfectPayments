package net.perfectdreams.i18nwrapper

import org.gradle.api.provider.Property

interface TranslationKeysGenPluginExtension {
    val generatedPackage: Property<String>
    val languageSourceFolder: Property<String>
}