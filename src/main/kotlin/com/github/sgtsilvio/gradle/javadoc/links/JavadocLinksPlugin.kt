package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * @author Silvio Giebl
 */
class JavadocLinksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val javadocProvider = project.tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc::class.java)
        val javadocLinksName = JavaPlugin.JAVADOC_TASK_NAME + "Links"
        val javadocLinksProvider =
            project.tasks.register(javadocLinksName, JavadocLinksTask::class.java, javadocProvider)
        javadocProvider.configure { javadoc ->
            javadoc.dependsOn(javadocLinksProvider)
        }
    }
}