package org.springframework.rocket.core;

import org.apache.rocketmq.common.message.MessageExt;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiConsumer;

public interface RocketReceivingOperations {

    long DEFAULT_POLL_TIMEOUT_MILLIS = 5000;

    default List<MessageExt> receive(String topic) {
        return receive(topic, null);
    }
    List<MessageExt> receive(String topic, Long timeoutMillis);

    default void receiveAsync(String topic, BiConsumer<List<MessageExt>, Throwable> receiveConsumer) {
        receiveAsync(topic, null, receiveConsumer);
    }
    void receiveAsync(String topic, Long timeoutMillis, BiConsumer<List<MessageExt>, Throwable> receiveConsumer);


    default <T> List<T> receiveAndConvert(String topic, Type payloadType) {
        return receiveAndConvert(topic, payloadType, null);
    }
    <T> List<T> receiveAndConvert(String topic, Type payloadType, Long timeoutMillis);

    default <T> void receiveAndConvertAsync(String topic, Type payloadType, BiConsumer<List<T>, Throwable> receiveConsumer) {
        receiveAndConvertAsync(topic, payloadType, null, receiveConsumer);
    }
    <T> void receiveAndConvertAsync(String topic, Type payloadType, Long timeoutMillis, BiConsumer<List<T>, Throwable> receiveConsumer);
}
