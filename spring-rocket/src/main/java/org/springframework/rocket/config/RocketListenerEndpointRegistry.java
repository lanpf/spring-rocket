package org.springframework.rocket.config;

import org.springframework.rocket.listener.RocketMessageListenerContainer;

public class RocketListenerEndpointRegistry extends AbstractListenerEndpointRegistry<RocketMessageListenerContainer, RocketListenerEndpoint> {

    protected RocketListenerEndpointRegistry() {
        super(RocketMessageListenerContainer.class);
    }
}
