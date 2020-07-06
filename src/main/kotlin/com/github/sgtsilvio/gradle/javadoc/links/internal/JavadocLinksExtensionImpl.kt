package com.github.sgtsilvio.gradle.javadoc.links.internal

import com.github.sgtsilvio.gradle.javadoc.links.JavadocLinksExtension

/**
 * @author Silvio Giebl
 */
open class JavadocLinksExtensionImpl : JavadocLinksExtension {
    override var configuration: String = "compileClasspath"
}
