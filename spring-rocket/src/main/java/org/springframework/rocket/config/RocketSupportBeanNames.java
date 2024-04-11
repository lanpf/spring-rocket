package org.springframework.rocket.config;

public abstract class RocketSupportBeanNames {

    /**
     * The bean name of the internally managed Rocket listener annotation processor.
     */
    public static final String ROCKET_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.rocket.annotation.internalRocketListenerAnnotationProcessor";

    /**
     * The bean name of the internally managed Rocket listener endpoint registry.
     */
    public static final String ROCKET_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME =
            "org.springframework.rocket.config.internalRocketListenerEndpointRegistry";

    /**
     * The bean name of the default rocket template.
     */
    public static final String DEFAULT_ROCKET_TEMPLATE_BEAN_NAME = "rocketTemplate";

    /**
     * The bean name of the internally managed Rocket transactional annotation processor.
     */
    public static final String ROCKET_TRANSACTIONAL_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.rocket.annotation.internalRocketTransactionalAnnotationProcessor";

}