package com.github.sgtsilvio.gradle.javadoc.links

import com.github.sgtsilvio.gradle.javadoc.links.internal.JavadocLinksExtensionImpl
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.stream.Collectors

/**
 * @author Silvio Giebl
 */
class JavadocLinksPlugin : Plugin<Project> {

    companion object {
        const val NAME: String = "javadocLinks"
    }

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val extension =
            project.extensions.create(JavadocLinksExtension::class.java, NAME, JavadocLinksExtensionImpl::class.java)

        val javadocProvider = project.tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc::class.java)

        val javadocLinksProvider = project.tasks.register(NAME) { javadocLinksTask ->

            val javadocLinksConfiguration = project.configurations.create(NAME) { configuration ->
                configuration.isVisible = false
                configuration.isTransitive = false
                configuration.isCanBeResolved = true
                configuration.isCanBeConsumed = false
                configuration.attributes { attributes ->
                    attributes.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(Category::class.java, Category.DOCUMENTATION)
                    )
                    attributes.attribute(
                        DocsType.DOCS_TYPE_ATTRIBUTE,
                        project.objects.named(DocsType::class.java, DocsType.JAVADOC)
                    )
                }
            }

            val javadoc = javadocProvider.get()
            val options = javadoc.options as StandardJavadocDocletOptions
            val javaVersion = JavaVersion.current()
            val downloadAndLinkOffline = !javaVersion.isJava10Compatible

            options.links(
                if (javaVersion.isJava11Compatible) {
                    "https://docs.oracle.com/en/java/javase/${javaVersion.majorVersion}/docs/api/"
                } else {
                    "https://docs.oracle.com/javase/${javaVersion.majorVersion}/docs/api/"
                }
            )

            fun getOfflinePackageListLocation(group: String, name: String, version: String): String =
                "${project.buildDir}/$NAME/${group}/${name}/${version}"

            val configuration = project.configurations.getByName(extension.configuration)
            val dependencySet = configuration.allDependencies.stream()
                .map { dependency -> Pair(dependency.group, dependency.name) }
                .collect(Collectors.toSet())

            val compileClasspath = project.configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
            compileClasspath.incoming.resolutionResult.root.dependencies.forEach { dependencyResult ->
                if (dependencyResult !is ResolvedDependencyResult) {
                    throw GradleException("can not create javadoc link for unresolved dependency: $dependencyResult")
                }
                val selected = dependencyResult.selected
                val moduleId = selected.moduleVersion
                    ?: throw GradleException("can not create javadoc link for dependency without moduleVersion: $dependencyResult")
                if (!dependencySet.contains(Pair(moduleId.group, moduleId.name))) {
                    return@forEach
                }
                val url = "https://javadoc.io/doc/${moduleId.group}/${moduleId.name}/${moduleId.version}/"
                when (val componentId = selected.id) {
                    is ProjectComponentIdentifier -> {
                        options.linksOffline(
                            url,
                            getOfflinePackageListLocation(moduleId.group, moduleId.name, moduleId.version)
                        )
                        if (componentId.build.isCurrentBuild) {
                            javadocLinksConfiguration.dependencies.add(
                                project.dependencies.project(mapOf("path" to componentId.projectPath))
                            )
                        } else {
                            javadocLinksConfiguration.dependencies.add(
                                project.dependencies.create("${moduleId.group}:${moduleId.name}")
                            )
                        }
                    }
                    is ModuleComponentIdentifier -> {
                        if (downloadAndLinkOffline) {
                            options.linksOffline(
                                url,
                                getOfflinePackageListLocation(moduleId.group, moduleId.name, moduleId.version)
                            )
                        } else {
                            options.links(url)
                        }
                    }
                }
            }

            javadocLinksTask.inputs.files(javadocLinksConfiguration)

            javadocLinksTask.doLast {
                javadocLinksConfiguration.resolvedConfiguration.resolvedArtifacts.forEach { resolvedArtifact ->
                    project.copy { copySpec ->
                        copySpec.from(project.zipTree(resolvedArtifact.file)) { zipCopySpec ->
                            zipCopySpec.include("package-list")
                            zipCopySpec.include("element-list")
                        }
                        val id = resolvedArtifact.moduleVersion.id
                        copySpec.into(getOfflinePackageListLocation(id.group, id.name, id.version))
                    }
                }
                if (downloadAndLinkOffline) {
                    options.linksOffline?.forEach {
                        val packageListFile = File(it.packagelistLoc, "package-list")
                        if (!packageListFile.exists()) {
                            packageListFile.parentFile.mkdirs()
                            try {
                                URL("${it.extDocUrl}package-list").openStream().use { input ->
                                    packageListFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            } catch (ignored: IOException) {
                                try {
                                    URL("${it.extDocUrl}element-list").openStream().use { input ->
                                        packageListFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                } catch (ignored: IOException) {
                                    javadoc.logger.warn(
                                        "Neither package-list nor element-list found for {}",
                                        it.extDocUrl
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        javadocProvider.configure { javadoc ->
            javadoc.dependsOn(javadocLinksProvider)
        }
    }
}