package org.springframework.rocket.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.rocket.config.RocketListenerEndpointRegistry;
import org.springframework.rocket.config.RocketSupportBeanNames;

public class RocketBootstrapConfiguration implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(RocketSupportBeanNames.ROCKET_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            registry.registerBeanDefinition(
                    RocketSupportBeanNames.ROCKET_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
                    new RootBeanDefinition(RocketListenerAnnotationBeanPostProcessor.class));
        }

        if (!registry.containsBeanDefinition(RocketSupportBeanNames.ROCKET_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)) {
            registry.registerBeanDefinition(
                    RocketSupportBeanNames.ROCKET_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
                    new RootBeanDefinition(RocketListenerEndpointRegistry.class));
        }

        if (!registry.containsBeanDefinition(RocketSupportBeanNames.ROCKET_TRANSACTIONAL_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            registry.registerBeanDefinition(
                    RocketSupportBeanNames.ROCKET_TRANSACTIONAL_ANNOTATION_PROCESSOR_BEAN_NAME,
                    new RootBeanDefinition(RocketTransactionalAnnotationBeanPostProcessor.class));
        }
    }
}
