package org.springframework.rocket.support.converter;

import org.apache.rocketmq.common.message.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.rocket.support.MessageConverter;

import java.lang.reflect.Type;
import java.util.Map;

public interface MessagingMessageConverter extends MessageConverter {

    default org.springframework.messaging.Message<?> convert(Object payload, Map<String, Object> headers) {
        return convert(payload, headers, null);
    }

    org.springframework.messaging.Message<?> convert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor);

    default org.springframework.messaging.Message<?> toMessage(Message rocketMessage) {
        return toMessage(rocketMessage, null);
    }

    org.springframework.messaging.Message<?> toMessage(Message rocketMessage, Type payloadType);

    Message fromMessage(org.springframework.messaging.Message<?> springMessage, String topic);
}
