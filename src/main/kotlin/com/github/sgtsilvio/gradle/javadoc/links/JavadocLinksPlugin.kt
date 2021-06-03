package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * @author Silvio Giebl
 */
class JavadocLinksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)

        val javadocLinksProvider = project.tasks.register<JavadocLinksTask>(JavaPlugin.JAVADOC_TASK_NAME + "Links") {
            project.dependencies.components.all<JavadocLinksMetadataRule>()
        }
        project.tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
            dependsOn(javadocLinksProvider)
            options.optionFiles(javadocLinksProvider.get().javadocOptionsFile.get().asFile)
        }
    }
}