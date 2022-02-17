@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.metadata)
}

group = "com.github.sgtsilvio.gradle"
description = "Gradle plugin to ease defining Javadoc links"

metadata {
    readableName.set("Gradle Javadoc Links Plugin")
    license {
        apache2()
    }
    developers {
        register("SgtSilvio") {
            fullName.set("Silvio Giebl")
        }
    }
    github {
        org.set("SgtSilvio")
        repo.set("gradle-javadoc-links")
        issues()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("javadoc-links") {
            id = "$group.$name"
            displayName = metadata.readableName.get()
            description = project.description
            implementationClass = "$group.javadoc.links.JavadocLinksPlugin"
        }
    }
}

pluginBundle {
    website = metadata.url.get()
    vcsUrl = metadata.scm.get().url.get()
    tags = listOf("javadoc", "links")
}

testing {
    suites.named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter.get())
    }
}
