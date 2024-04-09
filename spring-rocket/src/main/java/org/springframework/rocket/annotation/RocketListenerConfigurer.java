package org.springframework.rocket.annotation;

import org.springframework.rocket.config.GenericListenerEndpointRegistrar;

@FunctionalInterface
public interface RocketListenerConfigurer {

    /**
     * Callback allowing a {@link org.springframework.rocket.config.RocketListenerEndpointRegistry
     * RocketListenerEndpointRegistry} and specific
     * {@link org.springframework.rocket.config.ListenerEndpoint RocketListenerEndpoint} instances to be registered against the given
     * {@link org.springframework.rocket.config.GenericListenerEndpointRegistrar}. The default
     * {@link org.springframework.rocket.config.ListenerContainerFactory RocketListenerContainerFactory}
     * can also be customized.
     * @param registrar the registrar to be configured
     */
    void configureRocketListeners(GenericListenerEndpointRegistrar registrar);
}
