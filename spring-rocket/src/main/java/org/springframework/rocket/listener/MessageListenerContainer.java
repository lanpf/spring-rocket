package org.springframework.rocket.listener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;

public interface MessageListenerContainer extends SmartLifecycle, DisposableBean {

    @Override
    default void destroy() {
        stop();
    }

    default void setAutoStartup(boolean autoStartup) {
        // empty
    }

    /**
     * Return the {@code group.id} property for this container whether specifically set on the
     * container or via a consumer property on the consumer factory.
     * @return the group id.
     */
    default String getGroupId() {
        throw new UnsupportedOperationException("This container does not support retrieving the group id");
    }

    /**
     * The 'id' attribute of a {@code @RocketListener} or the bean name for spring-managed
     * containers.
     * @return the id or bean name.
     */
    default String getListenerId() {
        throw new UnsupportedOperationException("This container does not support retrieving the listener id");
    }
}
