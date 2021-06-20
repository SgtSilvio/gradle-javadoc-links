# Gradle Javadoc Links Plugin

[![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=brightgreen&label=gradle%20plugin&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fgithub%2Fsgtsilvio%2Fgradle%2Fjavadoc-links%2Fcom.github.sgtsilvio.gradle.javadoc-links.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.github.sgtsilvio.gradle.javadoc-links)

Gradle plugin to ease defining Javadoc links
- Links to JDK javadoc of the used java version
- Links to javadoc of dependencies (javadoc.io URLs are used by default)
- Links project dependencies offline which enables linking to unpublished versions
  - Works for subprojects (`include` in `settings.gradle`)
  - Works for dependencies substituted by included builds (`includeBuild` in `settings.gradle`)

## How to Use

```kotlin
plugins {
    id("com.github.sgtsilvio.gradle.javadoc-links") version "0.4.1"
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
