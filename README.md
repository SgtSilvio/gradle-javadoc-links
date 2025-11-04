# Gradle Javadoc Links Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.sgtsilvio.gradle.javadoc-links?color=brightgreen&style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.sgtsilvio.gradle.javadoc-links)
[![GitHub](https://img.shields.io/github/license/sgtsilvio/gradle-javadoc-links?color=brightgreen&style=for-the-badge)](LICENSE)
[![GitHub Workflow Status (with branch)](https://img.shields.io/github/actions/workflow/status/sgtsilvio/gradle-javadoc-links/check.yml?branch=main&style=for-the-badge)](https://github.com/SgtSilvio/gradle-javadoc-links/actions/workflows/check.yml?query=branch%3Amain)

Gradle plugin to ease defining Javadoc links
- Links to JDK javadoc of the used java version
- Links to javadoc of dependencies (javadoc.io URLs are used by default)
- Links project dependencies offline which enables linking to unpublished versions
  - Works for subprojects (`include` in `settings.gradle`)
  - Works for dependencies substituted by included builds (`includeBuild` in `settings.gradle`)

## How to Use

```kotlin
plugins {
    id("io.github.sgtsilvio.gradle.javadoc-links") version "0.9.0"
}
```

## Requirements

- Gradle 7.6 or higher

## Configuration

### URL

```kotlin
tasks.javadocLinks {
    urlProvider = { id -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/" }
}
```

### Dependencies

```kotlin
tasks.javadocLinks {
    useConfiguration(configurations.apiElements.get())
}
```
