package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * @author Silvio Giebl
 */
class JavadocLinksPlugin : Plugin<Project> {

    companion object {
        const val TASK_NAME = JavaPlugin.JAVADOC_TASK_NAME + "Links"
        const val CONFIGURATION_NAME = TASK_NAME
    }

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class)

        project.configurations.create(CONFIGURATION_NAME) {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.JAVADOC))
            }
            extendsFrom(project.configurations[JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME])
        }

        val javadocLinksProvider = project.tasks.register<JavadocLinksTask>(TASK_NAME) {}

        project.tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
            dependsOn(javadocLinksProvider)
            options.optionFiles(javadocLinksProvider.get().javadocOptionsFile.get())
        }
    }
}