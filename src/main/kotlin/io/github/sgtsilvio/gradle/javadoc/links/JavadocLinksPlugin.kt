package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
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

        val configuration = project.configurations.create(CONFIGURATION_NAME) {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.JAVADOC))
            }
            extendsFrom(project.configurations[JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME])
        }

        val javadoc = project.tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME)

        val javadocLinksTask = project.tasks.register<JavadocLinksTask>(TASK_NAME) {
            useConfiguration(configuration)
            javaVersion.set(javadoc.flatMap { it.javadocTool }.map { it.metadata.languageVersion })
        }

        javadoc {
            dependsOn(javadocLinksTask)
            options.optionFiles(javadocLinksTask.get().javadocOptionsFile.get())
        }
    }
}

const val TASK_NAME = JavaPlugin.JAVADOC_TASK_NAME + "Links"
const val CONFIGURATION_NAME = TASK_NAME