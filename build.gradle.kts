plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

group = "com.github.sgtsilvio.gradle"
description = "Gradle plugin to ease defining Javadoc links"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("javadoc-links") {
            id = "$group.$name"
            displayName = "Gradle Javadoc links plugin"
            description = project.description
            implementationClass = "$group.javadoc.links.JavadocLinksPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/SgtSilvio/gradle-javadoc-links"
    vcsUrl = "https://github.com/SgtSilvio/gradle-javadoc-links.git"
    tags = listOf("javadoc", "links")
}
