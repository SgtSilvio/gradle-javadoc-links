package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

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

        val configuration = project.configurations.create(CONFIGURATION_NAME) {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.JAVADOC))
            }
            extendsFrom(project.configurations[JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME])
        }

        val javadoc = project.tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME)

        val javadocLinksTaskClass =
            if (GradleVersion.current() >= GradleVersion.version("7.4")) NewJavadocLinksTask::class
            else JavadocLinksTaskBefore_7_4::class
        val javadocLinksTask = project.tasks.register(TASK_NAME, javadocLinksTaskClass) {
            useConfiguration(configuration)
            javaVersion.set(javadoc.flatMap { it.javadocTool }.map { it.metadata.languageVersion })
        }

        javadoc {
            dependsOn(javadocLinksTask)
            options.optionFiles(javadocLinksTask.get().javadocOptionsFile.get())
        }
    }
}