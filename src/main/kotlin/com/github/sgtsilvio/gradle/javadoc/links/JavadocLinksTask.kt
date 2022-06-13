package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.get
import java.io.File

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksTask : AbstractJavadocLinksTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    protected val javadocJars: FileCollection

    @get:Input
    protected val moduleVersionIds: Provider<List<ModuleVersionIdentifier>>

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

    @TaskAction
    protected fun run() {
        val javadocJars = javadocJars.files
        val moduleVersionIds = moduleVersionIds.get()
        val javaVersion = javaVersion.get()
        val outputDirectory = outputDirectory.get()
        val javadocOptionsFile = javadocOptionsFile.get()

        val options = mutableListOf<String>()
        options += linkToStdLib(javaVersion)

        javadocJars.forEachIndexed { i, javadocJar ->
            val id = moduleVersionIds[i]
            options += linkToComponent(id, javadocJar, outputDirectory)
        }

        javadocOptionsFile.writeText(options.joinToString("\n"))
    }
}