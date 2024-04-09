package org.springframework.rocket.annotation;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * A {@link DeferredImportSelector} implementation with the lowest order to import a
 * {@link RocketBootstrapConfiguration} as late as possible.
 *
 */
public class RocketAnnotationConfigurationSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] { RocketBootstrapConfiguration.class.getName() };
    }
}
