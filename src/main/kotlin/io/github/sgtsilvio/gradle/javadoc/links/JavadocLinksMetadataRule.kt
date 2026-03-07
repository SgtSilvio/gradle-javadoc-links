package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.Named
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.*
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * @author Silvio Giebl
 */
abstract class JavadocLinksMetadataRule : ComponentMetadataRule {

    @get:Inject
    protected abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) = context.details.run {
        val hasLibraryVariant = AtomicBoolean(false)
        val hasJavadocVariant = AtomicBoolean(false)
        allVariants {
            attributes {
                when (get(Category.CATEGORY_ATTRIBUTE)) {
                    Category.LIBRARY -> hasLibraryVariant.set(true)
                    Category.DOCUMENTATION -> {
                        if (get(DocsType.DOCS_TYPE_ATTRIBUTE) == DocsType.JAVADOC) {
                            hasJavadocVariant.set(true)
                        }
                    }
                }
            }
        }
        addVariant("javadocLinkElements") {
            attributes {
                if (hasLibraryVariant.get() && !hasJavadocVariant.get()) {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                } else {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named("ignore"))
                }
            }
            withFiles {
                addFile("${id.name}-${id.version}-javadoc.jar")
            }
        }
    }
}

private fun <T : Named> AttributeContainer.get(key: Attribute<T>): String? =
    getAttribute(key)?.name ?: getAttribute(Attribute.of(key.name, String::class.java))
