package org.springframework.rocket.config;

import org.springframework.rocket.listener.RocketMessageListenerContainer;

import java.util.Properties;

public interface RocketListenerEndpoint extends ListenerEndpoint<RocketMessageListenerContainer> {

    /**
     * Return the current batch listener flag for this endpoint
     * @return the batch listener flag.
     */
    Boolean getBatchListener();

    /**
     * Get the consumer properties that will be merged with the consumer properties
     * provided by the consumer factory; properties here will supersede any with the same
     * name(s) in the consumer factory.
     * {@code consumerGroup} are ignored.
     * @return the properties.
     * @see org.apache.rocketmq.client.consumer.DefaultMQPushConsumer
     * @see #getGroupId()
     */
    Properties getConsumerProperties();

}
