package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
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
    @get:PathSensitive(PathSensitivity.NONE)
    protected val javadocJars: FileCollection

    @get:Input
    protected val moduleVersionIds: Provider<List<ModuleVersionIdentifier>>

    @get:OutputDirectory
    protected val outputDirectory = project.provider { temporaryDir }

    @get:Internal
    internal val javadocOptionsFile = outputDirectory.map { it.resolve("javadoc.options") }

    init {
        val configuration = project.configurations[JavadocLinksPlugin.CONFIGURATION_NAME]
        javadocJars = configuration
        moduleVersionIds = project.provider {
            val map = mutableMapOf<File, ModuleVersionIdentifier>()
            for (resolvedArtifact in configuration.resolvedConfiguration.resolvedArtifacts) {
                map[resolvedArtifact.file] = resolvedArtifact.moduleVersion.id
            }
            javadocJars.files.map { map[it]!! }
        }
    }

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    @TaskAction
    protected fun run() {
        val javadocJars = javadocJars.files
        val moduleVersionIds = moduleVersionIds.get()
        val outputDirectory = outputDirectory.get()
        val javadocOptionsFile = javadocOptionsFile.get()

        val options = mutableListOf<String>()

        val javaVersion = JavaVersion.current()
        options += if (javaVersion.isJava11Compatible) {
            "-link https://docs.oracle.com/en/java/javase/${javaVersion.majorVersion}/docs/api/"
        } else {
            "-link https://docs.oracle.com/javase/${javaVersion.majorVersion}/docs/api/"
        }

        javadocJars.forEachIndexed { i, javadocJar ->
            val id = moduleVersionIds[i]
            val url = urlProvider.invoke(id)
            val offlineLocation = outputDirectory.resolve("${id.group}/${id.name}/${id.version}")

            options += "-linkoffline $url $offlineLocation"

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
        }

        javadocOptionsFile.writeText(options.joinToString("\n"))
    }
}