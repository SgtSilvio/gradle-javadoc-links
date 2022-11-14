# Gradle Javadoc Links Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.sgtsilvio.gradle.javadoc-links?color=brightgreen&style=for-the-badge)](https://plugins.gradle.org/plugin/com.github.sgtsilvio.gradle.javadoc-links)
[![GitHub](https://img.shields.io/github/license/sgtsilvio/gradle-javadoc-links?color=brightgreen&style=for-the-badge)](LICENSE)
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/sgtsilvio/gradle-javadoc-links/CI%20Check/master?style=for-the-badge)](https://github.com/SgtSilvio/gradle-javadoc-links/actions/workflows/check.yml?query=branch%3Amaster)

Gradle plugin to ease defining Javadoc links
- Links to JDK javadoc of the used java version
- Links to javadoc of dependencies (javadoc.io URLs are used by default)
- Links project dependencies offline which enables linking to unpublished versions
  - Works for subprojects (`include` in `settings.gradle`)
  - Works for dependencies substituted by included builds (`includeBuild` in `settings.gradle`)

## How to Use

```kotlin
plugins {
    id("com.github.sgtsilvio.gradle.javadoc-links") version "0.5.0"
}
```

## Requirements

- Gradle 6.6 or higher

## Configuration

### URL

```kotlin
tasks.javadocLinks {
    urlProvider = { id -> "https://javadoc.io/doc/${id.group}/${id.name}/${id.version}/" }
}
```

### Dependencies

```kotlin
configurations.javadocLinks {
    setExtendsFrom(listOf(configurations.apiElements.get()))
}
```
