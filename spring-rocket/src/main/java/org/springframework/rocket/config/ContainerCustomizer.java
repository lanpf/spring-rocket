package org.springframework.rocket.config;

import org.springframework.rocket.listener.MessageListenerContainer;

@FunctionalInterface
public interface ContainerCustomizer<C extends MessageListenerContainer> {

    void configure(C container);
}
