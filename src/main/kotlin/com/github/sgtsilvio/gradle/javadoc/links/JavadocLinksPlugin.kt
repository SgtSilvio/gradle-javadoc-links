package com.github.sgtsilvio.gradle.javadoc.links

import com.github.sgtsilvio.gradle.javadoc.links.internal.JavadocLinksExtensionImpl
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
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

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            JavadocLinksExtension::class.java,
            "javadocLinks",
            JavadocLinksExtensionImpl::class.java
        )
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

            fun getJavadocIoLink(dependency: Dependency): String =
                "https://javadoc.io/doc/${dependency.group}/${dependency.name}/${dependency.version}/"

            fun getPackageListDir(dependency: Dependency): String =
                "${project.buildDir}/javadocLinks/${dependency.group}/${dependency.name}/${dependency.version}"

            project.configurations.getByName(extension.configuration).allDependencies.forEach {
                val link = getJavadocIoLink(it)
                when (it) {
                    is ProjectDependency -> {
                        val task = it.dependencyProject.tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc::class.java)
                        javadoc.dependsOn(task)
                        options.linksOffline(link, task.get().destinationDir?.path)
                    }
                    is ExternalDependency -> {
                        if (downloadAndLinkOffline) {
                            options.linksOffline(link, getPackageListDir(it))
                        } else {
                            options.links(link)
                        }
                    }
                }
            }

            if (downloadAndLinkOffline) {
                javadoc.doFirst("javadocLinks") {
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