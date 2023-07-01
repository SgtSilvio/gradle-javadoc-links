package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

/**
 * @author Silvio Giebl
 */
abstract class AbstractJavadocLinksTask : DefaultTask() {

    private companion object {
        const val ELEMENT_LIST_NAME = "element-list"
        const val PACKAGE_LIST_NAME = "package-list"
    }

    @get:Input
    var urlProvider: (ModuleVersionIdentifier) -> String =
        { id -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/" }

    @get:Input
    val javaVersion: Property<JavaLanguageVersion> = project.objects.property<JavaLanguageVersion>()
        .convention(JavaLanguageVersion.of(JavaVersion.current().majorVersion))

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    protected val javadocJars: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectory
    protected val outputDirectory: Provider<File> = project.provider { temporaryDir }

    @get:Internal
    internal val javadocOptionsFile: Provider<File> = outputDirectory.map { it.resolve("javadoc.options") }

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    abstract fun useConfiguration(configuration: Configuration)

    protected fun linkToStdLib(javaVersion: JavaLanguageVersion) = if (javaVersion.asInt() >= 11) {
        "-link https://docs.oracle.com/en/java/javase/$javaVersion/docs/api/"
    } else {
        "-link https://docs.oracle.com/javase/$javaVersion/docs/api/"
    }

    protected fun linkToComponent(id: ModuleVersionIdentifier, javadocJar: File, outputDirectory: File): String {
        val url = urlProvider.invoke(id)
        val offlineLocation = outputDirectory.resolve("${id.group}/${id.name}/${id.version}")

        fileSystemOperations.copy {
            from(archiveOperations.zipTree(javadocJar)) { include(ELEMENT_LIST_NAME, PACKAGE_LIST_NAME) }
            into(offlineLocation)
        }
        val elementListFile = offlineLocation.resolve(ELEMENT_LIST_NAME)
        val packageListFile = offlineLocation.resolve(PACKAGE_LIST_NAME)
        if (elementListFile.exists() && !packageListFile.exists()) {
            elementListFile.copyTo(packageListFile)
        } else if (!elementListFile.exists() && packageListFile.exists()) {
            packageListFile.copyTo(elementListFile)
        }

        return "-linkoffline $url $offlineLocation"
    }
}