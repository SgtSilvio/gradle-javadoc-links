package io.github.sgtsilvio.gradle.javadoc.links

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
internal class MinRequiredGradleVersionTest {

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
    fun projectAndIncludedBuildAndExternalDependenciesWork() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test"
            include("sub-test")
            project(":sub-test").projectDir = file("sub-project")
            includeBuild("../included-project")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            $$"""
            plugins {
                `java-library`
                id("io.github.sgtsilvio.gradle.javadoc-links")
            }
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(11))
                }
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                api(project(":sub-test")) // subproject, javadocElements configuration
                api("group:included-test") // included project, javadocElements configuration
                api("com.hivemq:hivemq-extension-sdk:4.7.0") // gradle metadata, javadocElements variant
                api("io.netty:netty-handler:4.1.68.Final") // no gradle metadata, no javadocElements variant
                api("io.reactivex.rxjava3:rxjava:3.1.8") // gradle metadata, no javadocElements variant
                api(platform("io.dropwizard.metrics:metrics-bom:4.2.37")) // platform is ignored
                api("io.dropwizard.metrics:metrics-core") // uses version from platform
            }
            tasks.javadocLinks {
                urlProvider = { id ->
                    when (id.group) {
                        "group" -> "https://group.com/${id.name}/${id.version}/"
                        else -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/"
                    }
                }
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
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(11))
                }
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
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(11))
                }
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
            .withGradleVersion("7.6")
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("javadoc")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":sub-test:javadocJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":included-project:javadocJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocLinks")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadoc")?.outcome)
        assertJavadocLinksFiles()
    }

    private fun assertJavadocLinksFiles() {
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
        assertTrue(buildDir.resolve("io.reactivex.rxjava3/rxjava/3.1.8/element-list").exists())
        assertTrue(buildDir.resolve("io.reactivex.rxjava3/rxjava/3.1.8/package-list").exists())
        val lines = buildDir.resolve("javadoc.options").readLines()
        assertEquals(lines[0], "-link https://docs.oracle.com/en/java/javase/11/docs/api/")
        assertTrue(lines[1].matches(Regex("-linkoffline https://group\\.com/sub-test/0\\.1\\.0/ .*/build/tmp/javadocLinks/group/sub-test/0\\.1\\.0")))
        assertTrue(lines[2].matches(Regex("-linkoffline https://group\\.com/included-test/0\\.1\\.0/ .*/build/tmp/javadocLinks/group/included-test/0\\.1\\.0")))
        assertTrue(lines[3].matches(Regex("-linkoffline https://javadoc\\.io/doc/com\\.hivemq/hivemq-extension-sdk/4\\.7\\.0/ .*/build/tmp/javadocLinks/com\\.hivemq/hivemq-extension-sdk/4\\.7\\.0")))
        assertTrue(lines[4].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-handler/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-handler/4\\.1\\.68\\.Final")))
        assertTrue(lines[5].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.reactivex\\.rxjava3/rxjava/3\\.1\\.8/ .*/build/tmp/javadocLinks/io\\.reactivex\\.rxjava3/rxjava/3\\.1\\.8")))
        assertTrue(lines[6].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.dropwizard\\.metrics/metrics-core/4\\.2\\.37/ .*/build/tmp/javadocLinks/io\\.dropwizard\\.metrics/metrics-core/4\\.2\\.37")))
        assertTrue(lines[7].matches(Regex("-linkoffline https://javadoc\\.io/doc/org\\.slf4j/slf4j-api/1\\.7\\.36/ .*/build/tmp/javadocLinks/org\\.slf4j/slf4j-api/1\\.7\\.36")))
        assertTrue(lines[8].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-codec/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-codec/4\\.1\\.68\\.Final")))
        assertTrue(lines[9].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-transport/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-transport/4\\.1\\.68\\.Final")))
        assertTrue(lines[10].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-resolver/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-resolver/4\\.1\\.68\\.Final")))
        assertTrue(lines[11].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-buffer/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-buffer/4\\.1\\.68\\.Final")))
        assertTrue(lines[12].matches(Regex("-linkoffline https://javadoc\\.io/doc/io\\.netty/netty-common/4\\.1\\.68\\.Final/ .*/build/tmp/javadocLinks/io\\.netty/netty-common/4\\.1\\.68\\.Final")))
        assertTrue(lines[13].matches(Regex("-linkoffline https://javadoc\\.io/doc/org\\.reactivestreams/reactive-streams/1\\.0\\.4/ .*/build/tmp/javadocLinks/org\\.reactivestreams/reactive-streams/1\\.0\\.4")))
        assertEquals(14, lines.size)
    }
}