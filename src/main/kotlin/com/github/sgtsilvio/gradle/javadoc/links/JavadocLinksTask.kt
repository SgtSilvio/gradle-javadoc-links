package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import java.util.function.Function
import javax.inject.Inject

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksTask : DefaultTask() {

    @get:InputFiles
    val configuration: Configuration = project.configurations.create(name) {
        isVisible = false
        isTransitive = false
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class, Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType::class, DocsType.JAVADOC))
        }
        extendsFrom(project.configurations[JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME])
    }

    @get:Internal
    var urlProvider = Function<ModuleVersionIdentifier, String> { moduleVersionId ->
        "https://javadoc.io/doc/${moduleVersionId.group}/${moduleVersionId.name}/${moduleVersionId.version}/"
    }

    @get:OutputFile
    val javadocOptionsFile =
        project.objects.fileProperty().convention(project.layout.buildDirectory.file("javadocLinks/javadoc.options"))

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    protected fun run() {
        val options = mutableListOf<String>()

        val javaVersion = JavaVersion.current()
        options += if (javaVersion.isJava11Compatible) {
            "-link https://docs.oracle.com/en/java/javase/${javaVersion.majorVersion}/docs/api/"
        } else {
            "-link https://docs.oracle.com/javase/${javaVersion.majorVersion}/docs/api/"
        }

        for (resolvedArtifact in configuration.resolvedConfiguration.resolvedArtifacts) {
            val moduleVersionId = resolvedArtifact.moduleVersion.id
            val url = urlProvider.apply(moduleVersionId)
            val offlineLocation =
                temporaryDir.resolve("${moduleVersionId.group}/${moduleVersionId.name}/${moduleVersionId.version}")

            options += "-linkoffline $url $offlineLocation"

            fileSystemOperations.copy {
                from(project.zipTree(resolvedArtifact.file)) {
                    include("package-list")
                    include("element-list")
                }
                into(offlineLocation)
            }
        }

        javadocOptionsFile.get().asFile.writeText(options.joinToString("\n"))
    }
}