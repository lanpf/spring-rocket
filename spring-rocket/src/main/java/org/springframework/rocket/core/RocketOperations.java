package org.springframework.rocket.core;

import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.messaging.Message;

import java.lang.reflect.Type;
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


    /**
     * send oneway spring message
     */
    void sendOneway(String topic, Message<?> message);


    /**
     * send and receive(request/reply) spring message
     */
    default <T> T sendAndReceive(String topic, Message<?> message, Type replyPayloadType) {
        return sendAndReceive(topic, message, replyPayloadType, null);
    }
    <T> T sendAndReceive(String topic, Message<?> message, Type replyPayloadType, Long timeoutMillis);


    /**
     * async send and receive(request/reply) spring message
     */
    default <T> void sendAndReceiveAsync(String topic, Message<?> message, Type replyPayloadType, BiConsumer<T, Throwable> replyConsumer) {
        sendAndReceiveAsync(topic, message, replyPayloadType, null, replyConsumer);
    }
    <T> void sendAndReceiveAsync(String topic, Message<?> message, Type replyPayloadType, Long timeoutMillis, BiConsumer<T, Throwable> replyConsumer);
}
