
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * Converts a SASS file to a CSS file and puts the converted file into `build/sass/style.css`
 *
 * The Dart Sass implementation will be downloaded, based on your current operating system, after that, your SASS file will be converted to CSS.
 *
 * Don't forget to include the generated CSS file into your resources!
 *
 * ```
 * from(File(buildDir, "sass")) {
 *   into("static/v3/assets/css/")
 * }
 * ```
 *
 * This is just a hacky workaround, because the Gradle task we were using before isn't supported in Gradle 7, however this is a nice workaround that seems to work fine :)
 *
 * @param inputSass the file that will be converted
 */
fun Project.sassTask(inputSass: String): Task {
    return task("sass") {
        val operatingSystem = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

        val isWindows = operatingSystem.internalOs.isWindows
        val isLinux = operatingSystem.internalOs.isLinux

        val folderName = when {
            isWindows -> "windows"
            isLinux -> "linux"
            else -> throw UnsupportedOperationException("Unsupported OS $operatingSystem! The sassTask code must be updated to support it!")
        }

        val url = when {
            isWindows -> "https://github.com/sass/dart-sass/releases/download/1.35.2/dart-sass-1.35.2-windows-x64.zip"
            isLinux -> "https://github.com/sass/dart-sass/releases/download/1.35.2/dart-sass-1.35.2-linux-x64.tar.gz"
            else -> throw UnsupportedOperationException("Unsupported OS $operatingSystem! The sassTask code must be updated to support it!")
        }

        doLast {
            // The "caches" folder is used by Paperweight, so that's why I used the same name
            val dartSassTempFolder = File(project.rootDir, ".gradle/caches/dart-sass")
            dartSassTempFolder.mkdirs()

            val dartSassOsTempFolder = File(dartSassTempFolder, folderName)

            if (!dartSassOsTempFolder.exists()) {
                dartSassOsTempFolder.mkdirs()
                println("Downloading SASS from $url... Hang tight!")
                val sass = URL(url).readBytes()

                val extension = if (url.endsWith(".zip"))
                    "zip"
                else
                    "tar.gz"

                // Write the file...
                val sassZipFile = File(dartSassTempFolder, "sass.$extension")
                sassZipFile.writeBytes(sass)

                // And then extract it!
                if (extension == "zip") {
                    extractZip(sassZipFile, dartSassOsTempFolder)
                } else {
                    extractTarGz(sassZipFile, dartSassOsTempFolder)
                }
            }

            // Execute SASS
            when {
                isWindows -> {
                    val originalSassLocation = File(project.projectDir, "src/main/sass/$inputSass")
                    val buildSassFolder = File(project.projectDir, "build/sass/")
                    buildSassFolder.mkdirs()
                    val outputSassLocation = File(buildSassFolder, "style.css")

                    val pb = ProcessBuilder("cmd.exe", "/C", "$dartSassOsTempFolder\\dart-sass\\sass.bat", originalSassLocation.toString(), outputSassLocation.toString())
                        .start()

                    val status = pb.waitFor()

                    println(pb.inputStream.readAllBytes().toString(Charsets.UTF_8))

                    println("Process Status: $status")
                }
                isLinux -> {
                    println("$dartSassOsTempFolder/dart-sass/sass")

                    val originalSassLocation = File(project.projectDir, "src/main/sass/$inputSass")
                    val buildSassFolder = File(project.projectDir, "build/sass/")
                    buildSassFolder.mkdirs()
                    val outputSassLocation = File(buildSassFolder, "style.css")

                    val pb = ProcessBuilder("$dartSassOsTempFolder/dart-sass/sass", originalSassLocation.toString(), outputSassLocation.toString())
                        .start()

                    val status = pb.waitFor()

                    println(pb.inputStream.readAllBytes().toString(Charsets.UTF_8))

                    println("Process Status: $status")
                }
                else -> {
                    throw UnsupportedOperationException("Unsupported OS $operatingSystem! The sassTask code must be updated to support it!")
                }
            }

            println("Done!")
        }
    }
}

private fun extractZip(input: File, output: File) {
    ZipFile(input).use { zipFile ->
        val entries: Enumeration<out ZipEntry?> = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry: ZipEntry = entries.nextElement() ?: return
            val entryDestination = File(output, entry.name)
            if (entry.isDirectory) {
                entryDestination.mkdirs()
            } else {
                entryDestination.parentFile.mkdirs()
                zipFile.getInputStream(entry).use { `in` ->
                    FileOutputStream(entryDestination).use { out ->
                        zipFile.getInputStream(entry).transferTo(out)
                    }
                }
            }
        }
    }
}

private fun extractTarGz(input: File, output: File) {
    // Pray that the user has sh and tar installed
    val builder = ProcessBuilder()
    builder.command("sh", "-c", java.lang.String.format("tar xfz %s -C %s", input, output))
    builder.directory(File("/tmp"))
    val process = builder.start()
    process.waitFor()
}