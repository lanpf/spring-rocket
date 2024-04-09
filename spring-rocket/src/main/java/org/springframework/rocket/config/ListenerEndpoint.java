package org.springframework.rocket.config;

import org.springframework.rocket.listener.MessageListenerContainer;
import org.springframework.rocket.support.MessageConverter;

public interface ListenerEndpoint<C extends MessageListenerContainer> {

    /**
     * Return the id of this endpoint.
     * @return the id of this endpoint. The id can be further qualified
     * when the endpoint is resolved against its actual listener
     * container.
     * @see ListenerContainerFactory#createListenerContainer
     */
    String getId();
    /**
     * Return the groupId of this endpoint - if present, overrides the
     * {@code consumerGroup} property of the consumer factory.
     * @return the group id; may be null.
     */
    String getGroupId();
    /**
     * Return the topic for this endpoint.
     * @return the topic for this endpoint.
     */
    String getTopic();
    /**
     * Return the topic filter expression type(TAG/SQL92) for this endpoint.
     * @return the topic filter expression type for this endpoint.
     */
    String getFilterExpressionType();
    /**
     * Return the topic filter expression for this endpoint.
     * @return the topic filter expression for this endpoint.
     */
    String getFilterExpression();
    /**
     * Return the autoStartup for this endpoint's container.
     * @return the autoStartup.
     */
    Boolean getAutoStartup();
    /**
     * Return the concurrency for this endpoint's container.
     * @return the concurrency.
     */
    Boolean getConcurrency();

    /**
     * Setup the specified message listener container with the model defined by this
     * endpoint.
     * <p>
     * This endpoint must provide the requested missing option(s) of the specified
     * container to make it usable. Usually, this is about setting the {@code queues} and
     * the {@code messageListener} to use but an implementation may override any default
     * setting that was already set.
     * @param listenerContainer the listener container to configure
     * @param messageConverter the message converter - can be null
     */
    void setupListenerContainer(C listenerContainer, MessageConverter messageConverter);
}
