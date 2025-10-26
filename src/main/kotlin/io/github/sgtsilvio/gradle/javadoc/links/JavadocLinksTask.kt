package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultTask() {

    private companion object {
        const val ELEMENT_LIST_NAME = "element-list"
        const val PACKAGE_LIST_NAME = "package-list"
    }

    @get:Input
    var urlProvider: (ModuleVersionIdentifier) -> String =
        { id -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/" }

    @get:Input
    val javaVersion = project.objects.property<JavaLanguageVersion>()
        .convention(JavaLanguageVersion.of(JavaVersion.current().majorVersion))

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    protected val javadocJars = project.objects.fileCollection()

    @get:Input
    protected val artifactIds = project.objects.listProperty<ComponentArtifactIdentifier>()

    @get:Input
    protected val rootComponent = project.objects.property<ResolvedComponentResult>()

    @get:OutputDirectory
    protected val outputDirectory: Provider<File> = project.provider { temporaryDir }

    @get:Internal
    internal val javadocOptionsFile: Provider<File> = outputDirectory.map { it.resolve("javadoc.options") }

    fun useConfiguration(configuration: Configuration) {
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

    private fun linkToStdLib(javaVersion: JavaLanguageVersion) = when {
        javaVersion.asInt() >= 11 -> "-link https://docs.oracle.com/en/java/javase/$javaVersion/docs/api/"
        else -> "-link https://docs.oracle.com/javase/$javaVersion/docs/api/"
    }

    private fun linkToComponent(id: ModuleVersionIdentifier, javadocJar: File, outputDirectory: File): String {
        val url = urlProvider(id)
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

    private inline fun iterateComponents(
        rootComponent: ResolvedComponentResult,
        action: (ResolvedComponentResult) -> Unit,
    ) {
        val processedComponents = HashSet<ResolvedComponentResult>()
        val componentsToProcess = LinkedList<ResolvedComponentResult>()
        for (dependency in rootComponent.dependencies) {
            componentsToProcess.offer((dependency as ResolvedDependencyResult).selected)
        }
        while (componentsToProcess.isNotEmpty()) {
            val component = componentsToProcess.poll()
            if (processedComponents.add(component)) {
                action(component)
                for (dependency in component.dependencies) {
                    componentsToProcess.offer((dependency as ResolvedDependencyResult).selected)
                }
            }
        }
    }
}