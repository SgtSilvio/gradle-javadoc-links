package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.util.*

/**
 * @author Silvio Giebl
 */
abstract class NewJavadocLinksTask : AbstractJavadocLinksTask() {

    @get:Input
    protected val artifactIds = project.objects.listProperty<ComponentArtifactIdentifier>()

    @get:Input
    protected val rootComponent = project.objects.property<ResolvedComponentResult>()

    override fun useConfiguration(configuration: Configuration) {
        val artifacts = configuration.incoming.artifacts
        javadocJars.setFrom(artifacts.artifactFiles)
        artifactIds.set(artifacts.resolvedArtifacts.map { results -> results.map { result -> result.id } })
        rootComponent.set(configuration.incoming.resolutionResult.rootComponent)
    }

    @TaskAction
    protected fun run() {
        val javaVersion = javaVersion.get()
        val javadocJars = javadocJars.files
        val artifactIds = artifactIds.get()
        val rootComponent = rootComponent.get()
        val outputDirectory = outputDirectory.get()
        val javadocOptionsFile = javadocOptionsFile.get()

        val options = mutableListOf<String>()
        options += linkToStdLib(javaVersion)

        val idToJavadocJar = artifactIds.map { it.componentIdentifier }.zip(javadocJars).toMap()
        iterateComponents(rootComponent) { component ->
            val id = component.moduleVersion!!
            val javadocJar = idToJavadocJar[component.id]!!
            options += linkToComponent(id, javadocJar, outputDirectory)
        }

        javadocOptionsFile.writeText(options.joinToString("\n"))
    }

    private inline fun iterateComponents(
        rootComponent: ResolvedComponentResult,
        action: (ResolvedComponentResult) -> Unit
    ) {
        val processedComponents = HashSet<ResolvedComponentResult>()
        val componentsToProcess = LinkedList<ResolvedComponentResult>()
        for (dependency in rootComponent.dependencies) {
            componentsToProcess.offer((dependency as ResolvedDependencyResult).selected)
        }
        while (componentsToProcess.isNotEmpty()) {
            val component = componentsToProcess.poll()
            if (processedComponents.add(component)) {
                action.invoke(component)
                for (dependency in component.dependencies) {
                    componentsToProcess.offer((dependency as ResolvedDependencyResult).selected)
                }
            }
        }
    }
}