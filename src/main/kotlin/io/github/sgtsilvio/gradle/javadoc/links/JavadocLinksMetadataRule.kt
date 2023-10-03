package io.github.sgtsilvio.gradle.javadoc.links

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
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
        val existing = AtomicBoolean()
        withVariant(JavaPlugin.JAVADOC_ELEMENTS_CONFIGURATION_NAME) {
            attributes {
                existing.set(true)
            }
        }
        // javadoc variant derived from maven metadata
        // https://github.com/gradle/gradle/blob/c5566ea98bdc48ca42c1bcedae04cc9bd3cd9b1d/subprojects/dependency-management/src/main/java/org/gradle/internal/component/external/model/JavaEcosystemVariantDerivationStrategy.java#L60
        withVariant("javadoc") {
            attributes {
                existing.set(true)
            }
        }
        addVariant("javadocLinkElements") {
            attributes {
                if (!existing.get()) {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                }
            }
            withFiles {
                addFile("${id.name}-${id.version}-javadoc.jar")
            }
        }
    }
}