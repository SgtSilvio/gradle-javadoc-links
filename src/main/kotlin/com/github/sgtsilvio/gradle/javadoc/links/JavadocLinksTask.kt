package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File
import javax.inject.Inject

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksTask : DefaultTask() {

    private companion object {
        const val ELEMENT_LIST_NAME = "element-list"
        const val PACKAGE_LIST_NAME = "package-list"
    }

    @get:Internal
    var urlProvider: (ModuleVersionIdentifier) -> String =
        { id -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/" }

    @get:InputFiles
    protected val javadocJars: FileCollection

    @get:Internal
    protected val idToJavadocJars: Provider<Map<ModuleVersionIdentifier, File>>

    @get:OutputDirectory
    protected val outputDirectory = project.provider { temporaryDir }

    @get:Internal
    internal val javadocOptionsFile = outputDirectory.map { it.resolve("javadoc.options") }

    init {
        val configuration = project.configurations[JavadocLinksPlugin.CONFIGURATION_NAME]
        javadocJars = configuration
        idToJavadocJars = project.provider {
            val map = mutableMapOf<ModuleVersionIdentifier, File>()
            for (resolvedArtifact in configuration.resolvedConfiguration.resolvedArtifacts) {
                map[resolvedArtifact.moduleVersion.id] = resolvedArtifact.file
            }
            map
        }
    }

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    @TaskAction
    protected fun run() {
        val idToJavadocJars = idToJavadocJars.get()
        val outputDirectory = outputDirectory.get()
        val javadocOptionsFile = javadocOptionsFile.get()

        val options = mutableListOf<String>()

        val javaVersion = JavaVersion.current()
        options += if (javaVersion.isJava11Compatible) {
            "-link https://docs.oracle.com/en/java/javase/${javaVersion.majorVersion}/docs/api/"
        } else {
            "-link https://docs.oracle.com/javase/${javaVersion.majorVersion}/docs/api/"
        }

        for (idToJavadocJar in idToJavadocJars.entries) {
            val id = idToJavadocJar.key
            val url = urlProvider.invoke(id)
            val offlineLocation = outputDirectory.resolve("${id.group}/${id.name}/${id.version}")

            options += "-linkoffline $url $offlineLocation"

            fileSystemOperations.copy {
                from(archiveOperations.zipTree(idToJavadocJar.value)) { include(ELEMENT_LIST_NAME, PACKAGE_LIST_NAME) }
                into(offlineLocation)
            }
            val elementListFile = offlineLocation.resolve(ELEMENT_LIST_NAME)
            val packageListFile = offlineLocation.resolve(PACKAGE_LIST_NAME)
            if (elementListFile.exists() && !packageListFile.exists()) {
                elementListFile.copyTo(packageListFile)
            } else if (!elementListFile.exists() && packageListFile.exists()) {
                packageListFile.copyTo(elementListFile)
            }
        }

        javadocOptionsFile.writeText(options.joinToString("\n"))
    }
}