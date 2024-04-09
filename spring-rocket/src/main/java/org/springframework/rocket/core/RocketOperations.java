package org.springframework.rocket.core;

import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.BiConsumer;

public interface RocketOperations {

    long DEFAULT_SEND_TIMEOUT_MILLIS = 3000;


    /**
     * sync send spring message
     */
    default SendResult send(String topic, Message<?> message) {
        return send(topic, message, null);
    }
    SendResult send(String topic, Message<?> message, Long timeoutMillis);


    /**
     * batch sync send spring messages
     */
    default <T extends Message<?>> SendResult sendBatch(String topic, List<T> messages) {
        return sendBatch(topic, messages, null);
    }
    <T extends Message<?>> SendResult sendBatch(String topic, List<T> messages, Long timeoutMillis);


    /**
     * async send spring message
     */
    default void sendAsync(String topic, Message<?> message, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topic, message, null, sendConsumer);
    }
    void sendAsync(String topic, Message<?> message, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer);


    /**
     * batch async send spring messages
     */
    default <T extends Message<?>> void sendBatchAsync(String topic, List<T> messages, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendBatchAsync(topic, messages, null, sendConsumer);
    }
    <T extends Message<?>> void sendBatchAsync(String topic, List<T> messages, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer);
}
