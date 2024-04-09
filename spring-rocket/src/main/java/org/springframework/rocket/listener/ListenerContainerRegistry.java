package org.springframework.rocket.listener;

import java.util.Collection;
import java.util.Set;

public interface ListenerContainerRegistry {

    /**
     * Return the listener container with the specified id or {@code null} if no such
     * container exists.
     * @param id the id of the container
     * @return the container or {@code null} if no container with that id exists
     * @see org.springframework.rocket.config.ListenerEndpoint#getId()
     * @see #getListenerContainerIds()
     */
    MessageListenerContainer getListenerContainer(String id);

    /**
     * Return the ids of the managed listener container instance(s).
     * @return the ids.
     * @see #getListenerContainer(String)
     */
    Set<String> getListenerContainerIds();

    /**
     * Return the managed listener container instance(s).
     * @return the managed listener container instance(s).
     * @see #getAllListenerContainers()
     */
    Collection<? extends MessageListenerContainer> getListenerContainers();

    /**
     * Return all listener container instances including those managed by this registry
     * and those declared as beans in the application context. Prototype-scoped containers
     * will be included. Lazy beans that have not yet been created will not be initialized
     * by a call to this method.
     * @return the listener container instance(s).
     * @see #getListenerContainers()
     */
    Collection<? extends MessageListenerContainer> getAllListenerContainers();
}
