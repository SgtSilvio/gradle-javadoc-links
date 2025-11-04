package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.*

/**
 * @author Silvio Giebl
 */
@Suppress("unused")
class JavadocLinksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class)

        project.dependencies.components.all<JavadocLinksMetadataRule>()

        val javadoc = project.tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME)

        val javadocLinksTask = project.tasks.register<JavadocLinksTask>(TASK_NAME) {
            useConfiguration(project.configurations[JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME])
            javaVersion.set(javadoc.flatMap { it.javadocTool }.map { it.metadata.languageVersion })
        }

        javadoc {
            dependsOn(javadocLinksTask)
            options.optionFiles(javadocLinksTask.get().javadocOptionsFile.get())
        }
    }
}

const val TASK_NAME = JavaPlugin.JAVADOC_TASK_NAME + "Links"
