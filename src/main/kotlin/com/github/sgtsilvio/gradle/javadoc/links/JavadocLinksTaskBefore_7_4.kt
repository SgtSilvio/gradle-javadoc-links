package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import java.io.File
import kotlin.collections.set

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksTaskBefore_7_4 : AbstractJavadocLinksTask() {

    @get:Input
    protected val moduleVersionIds = project.objects.listProperty<ModuleVersionIdentifier>()

    override fun useConfiguration(configuration: Configuration) {
        javadocJars.setFrom(configuration)
        moduleVersionIds.set(project.provider {
            val map = mutableMapOf<File, ModuleVersionIdentifier>()
            for (resolvedArtifact in configuration.resolvedConfiguration.resolvedArtifacts) {
                map[resolvedArtifact.file] = resolvedArtifact.moduleVersion.id
            }
            javadocJars.files.map { map[it]!! }
        })
    }

    @TaskAction
    protected fun run() {
        val javaVersion = javaVersion.get()
        val javadocJars = javadocJars.files
        val moduleVersionIds = moduleVersionIds.get()
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