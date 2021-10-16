package com.github.sgtsilvio.gradle.javadoc.links

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * @author Silvio Giebl
 */
class ConfigurationCacheTest {

    @TempDir
    lateinit var rootDir: File
    private lateinit var projectDir: File
    private lateinit var subProjectDir: File
    private lateinit var includedProjectDir: File

    @BeforeEach
    fun setUp() {
        projectDir = rootDir.resolve("project").apply { mkdir() }
        subProjectDir = projectDir.resolve("sub-project").apply { mkdir() }
        includedProjectDir = rootDir.resolve("included-project").apply { mkdir() }
    }

    @Test
    fun configurationCacheReused() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test"
            include("sub-test")
            project(":sub-test").projectDir = file("sub-project")
            includeBuild("../included-project")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("com.github.sgtsilvio.gradle.javadoc-links")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                api(project(":sub-test")) // subproject, javadocElements configuration
                api("group:included-test") // included project, javadocElements configuration
                api("com.hivemq:hivemq-extension-sdk:4.7.0") // gradle metadata, javadocElements variant
                api("io.netty:netty-handler:4.1.68.Final") // no gradle metadata, no javadocElements variant
            }
            """.trimIndent()
        )
        projectDir.resolve("src/main/java/test/Test.java").apply { parentFile.mkdirs() }.writeText(
            """
            package test;
            /**
             * 
             */
            public class Test {}
            """.trimIndent()
        )

        subProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
            }
            group = "group"
            version = "0.1.0"
            java {
                withJavadocJar()
            }
            """.trimIndent()
        )
        subProjectDir.resolve("src/main/java/test/sub/Test.java").apply { parentFile.mkdirs() }.writeText(
            """
            package test.sub;
            /**
             * 
             */
            public class Test {}
            """.trimIndent()
        )

        includedProjectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "included-test"
            """.trimIndent()
        )
        includedProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
            }
            group = "group"
            version = "0.1.0"
            java {
                withJavadocJar()
            }
            """.trimIndent()
        )
        includedProjectDir.resolve("src/main/java/test/included/Test.java").apply { parentFile.mkdirs() }.writeText(
            """
            package test.included;
            /**
             * 
             */
            public class Test {}
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("javadoc", "--configuration-cache")
            .build()

        assertTrue(result.output.contains("Configuration cache entry stored"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub-test:javadocJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":included-project:javadocJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocLinks")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadoc")?.outcome)

        val buildDir = projectDir.resolve("build/tmp/javadocLinks")
        assertTrue(buildDir.resolve("javadoc.options").exists())
        assertTrue(buildDir.resolve("group/sub-test/0.1.0/element-list").exists())
        assertTrue(buildDir.resolve("group/sub-test/0.1.0/package-list").exists())
        assertTrue(buildDir.resolve("group/included-test/0.1.0/element-list").exists())
        assertTrue(buildDir.resolve("group/included-test/0.1.0/package-list").exists())
        assertTrue(buildDir.resolve("com.hivemq/hivemq-extension-sdk/4.7.0/element-list").exists())
        assertTrue(buildDir.resolve("com.hivemq/hivemq-extension-sdk/4.7.0/package-list").exists())
        assertTrue(buildDir.resolve("io.netty/netty-handler/4.1.68.Final/element-list").exists())
        assertTrue(buildDir.resolve("io.netty/netty-handler/4.1.68.Final/package-list").exists())
        val lines = buildDir.resolve("javadoc.options").readLines()
        assertEquals(lines[0], "-link https://docs.oracle.com/en/java/javase/11/docs/api/")
        assertTrue(lines[1].matches(Regex("-linkoffline https://javadoc\\.io/doc/group/sub-test/0\\.1\\.0/ .*/build/tmp/javadocLinks/group/sub-test/0\\.1\\.0")))
        assertTrue(lines[2].matches(Regex("-linkoffline https://javadoc\\.io/doc/group/included-test/0\\.1\\.0/ .*/build/tmp/javadocLinks/group/included-test/0\\.1\\.0")))
        assertTrue(lines[3].matches(Regex("-linkoffline https://javadoc\\.io/doc/com\\.hivemq/hivemq-extension-sdk/4\\.7\\.0/ .*/build/tmp/javadocLinks/com\\.hivemq/hivemq-extension-sdk/4\\.7\\.0")))
        assertTrue(lines[4].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-handler/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-handler/4\\.1\\.68\\.Final")))

        val result2 = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("javadoc", "--configuration-cache")
            .build()

        assertTrue(result2.output.contains("Configuration cache entry reused"))
        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":sub-test:javadocJar")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":included-project:javadocJar")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":javadocLinks")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":javadoc")?.outcome)
    }
}