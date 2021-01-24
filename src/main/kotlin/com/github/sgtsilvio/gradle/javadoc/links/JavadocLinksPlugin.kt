package com.github.sgtsilvio.gradle.javadoc.links

import com.github.sgtsilvio.gradle.javadoc.links.internal.JavadocLinksExtensionImpl
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.io.File
import java.io.IOException
import java.net.URL

/**
 * @author Silvio Giebl
 */
class JavadocLinksPlugin : Plugin<Project> {

    companion object {
        const val NAME: String = "javadocLinks"
    }

    override fun apply(project: Project) {
        val extension =
            project.extensions.create(JavadocLinksExtension::class.java, NAME, JavadocLinksExtensionImpl::class.java)
        project.afterEvaluate {
            register(project, extension)
        }
    }

    private fun register(project: Project, extension: JavadocLinksExtension) {
        project.tasks.withType(Javadoc::class.java).configureEach { javadoc ->

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

            val configuration = project.configurations.getByName(extension.configuration)
            configuration.incoming.resolutionResult.root.dependencies.forEach { dependencyResult ->
                if (dependencyResult !is ResolvedDependencyResult) {
                    throw GradleException("can not create javadoc link for unresolved dependency: $dependencyResult")
                }
                val selected = dependencyResult.selected
                val moduleVersion = selected.moduleVersion
                    ?: throw GradleException("can not create javadoc link for dependency without moduleVersion: $dependencyResult")
                val url =
                    "https://javadoc.io/doc/${moduleVersion.group}/${moduleVersion.name}/${moduleVersion.version}/"
                when (val id = selected.id) {
                    is ProjectComponentIdentifier -> {
                        if (id.build.isCurrentBuild) {
                            val includedProject = project.project(id.projectPath)
                            val task = includedProject.tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc::class.java)
                            val packageListLoc = task.get().destinationDir!!.path
                            javadoc.dependsOn(task)
                            options.linksOffline(url, packageListLoc)
                        } else {
                            val includedBuild = project.gradle.includedBuild(id.projectName)
                            val task = includedBuild.task(id.projectPath + ":" + JavaPlugin.JAVADOC_TASK_NAME)
                            val packageListLoc = includedBuild.projectDir.resolve("build/docs/javadoc").path
                            javadoc.dependsOn(task)
                            options.linksOffline(url, packageListLoc)
                        }
                    }
                    is ModuleComponentIdentifier -> {
                        if (downloadAndLinkOffline) {
                            val packageListLoc = "${project.buildDir}/$NAME/${id.group}/${id.module}/${id.version}"
                            options.linksOffline(url, packageListLoc)
                        } else {
                            options.links(url)
                        }
                    }
                }
            }

            if (downloadAndLinkOffline) {
                javadoc.doFirst(NAME) {
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
    }
}