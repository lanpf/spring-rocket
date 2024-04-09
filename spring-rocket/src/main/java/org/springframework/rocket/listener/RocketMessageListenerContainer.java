package org.springframework.rocket.listener;

public sealed interface RocketMessageListenerContainer extends MessageListenerContainer permits AbstractRocketMessageListenerContainer {

    void setupMessageListener(Object messageListener);

    default ContainerProperties getContainerProperties() {
        throw new UnsupportedOperationException("This container doesn't support retrieving its properties");
    }

    /**
     * Pause this container before the next poll(). The next poll by the container will be
     * disabled as long as {@link #resume()} is not called.
     */
    default void pause() {
        throw new UnsupportedOperationException("This container doesn't support pause");
    }

    /**
     * Resume this container, if paused.
     */
    default void resume() {
        throw new UnsupportedOperationException("This container doesn't support resume");
    }
}
