package org.springframework.rocket.config;

import org.springframework.rocket.listener.MessageListenerContainer;

@FunctionalInterface
public interface ListenerContainerFactory<C extends MessageListenerContainer, E extends ListenerEndpoint<C>> {

    C createListenerContainer(E endpoint);
}
