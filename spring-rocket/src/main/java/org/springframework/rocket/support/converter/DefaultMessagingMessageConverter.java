package org.springframework.rocket.support.converter;

import lombok.Setter;
import org.apache.rocketmq.common.message.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rocket.support.header.DefaultRocketHeaderMapper;
import org.springframework.rocket.support.header.RocketHeaderMapper;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Setter
public class DefaultMessagingMessageConverter implements MessagingMessageConverter {

    public static final String CONVERSION_HINT_HEADER = "conversionHint";

    private RocketHeaderMapper headerMapper = new DefaultRocketHeaderMapper();
    private MessageConverter messagingConverter = new SimpleMessageConverter();


    @Override
    public org.springframework.messaging.Message<?> convert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
        MessageHeaders messageHeaders = null;
        Object conversionHint = (headers != null ? headers.get(CONVERSION_HINT_HEADER) : null);

        Map<String, Object> headersToUse = processHeadersToSend(headers);
        if (headersToUse != null) {
            messageHeaders = (headersToUse instanceof MessageHeaders springHeaders ? springHeaders : new MessageHeaders(headersToUse));
        }


        org.springframework.messaging.Message<?> message = this.messagingConverter instanceof SmartMessageConverter smartMessageConverter ?
                smartMessageConverter.toMessage(payload, messageHeaders, conversionHint) :
                this.messagingConverter.toMessage(payload, messageHeaders);
        if (message == null) {
            String payloadType = payload.getClass().getName();
            Object contentType = (messageHeaders != null ? messageHeaders.get(MessageHeaders.CONTENT_TYPE) : null);
            throw new MessageConversionException(String.format(
                    "Unable to convert payload with type='%s', contentType='%s', converter=[%s]"
                    , payloadType, contentType, this.messagingConverter));
        }
        if (postProcessor != null) {
            message = postProcessor.postProcessMessage(message);
        }
        return message;
    }

    /**
     * Provides access to the map of input headers before a send operation.
     * Subclasses can modify the headers and then return the same or a different map.
     * <p>This default implementation in this class returns the input map.
     * @param headers the headers to send (or {@code null} if none)
     * @return the actual headers to send (or {@code null} if none)
     */
    protected Map<String, Object> processHeadersToSend(Map<String, Object> headers) {
        if (headers == null) {
            return new HashMap<>(64);
        }
        if (headers.containsKey(MessageHeaders.CONTENT_TYPE)) {
            return headers;
        }
        Map<String, Object> springHeaders = new HashMap<>(headers);
        springHeaders.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);
        return springHeaders;
    }


    @Override
    public org.springframework.messaging.Message<?> toMessage(Message rocketMessage, Type payloadType) {
        Map<String, Object> headers = new HashMap<>(64);
        this.headerMapper.toSpringHeaders(rocketMessage, headers);
        headers.putIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);

        org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(extractAndConvertValue(rocketMessage, payloadType), new MessageHeaders(headers));
        if (payloadType != null && !Objects.equals(payloadType, byte[].class)) {
            Class<?> clazz;
            if (payloadType instanceof Class) {
                clazz = (Class<?>) payloadType;
            } else if (payloadType instanceof ParameterizedType parameterizedType) {
                clazz = (Class<?>) parameterizedType.getRawType();
            } else {
                clazz = Objects.class;
            }
            Object payload;
            if (this.messagingConverter instanceof SmartMessageConverter smartMessageConverter) {
                payload = smartMessageConverter.fromMessage(message, clazz, payloadType);
            } else {
                payload = this.messagingConverter.fromMessage(message, clazz);
            }
            if (payload != null) {
                message = new GenericMessage<>(payload, message.getHeaders());
            }
        }
        return message;
    }

    private Object extractAndConvertValue(Message rocketMessage, Type payloadType) {
        return rocketMessage.getBody();
    }

    @Override
    public Message fromMessage(org.springframework.messaging.Message<?> springMessage, String topic) {
        Assert.hasText(topic, "topic must not be null or empty");

        byte[] payload = convertPayload(springMessage);
        if (payload == null || payload.length == 0) {
            return null;
        }

        Message rocketMessage = new Message(topic, payload);
        this.headerMapper.fromSpringHeaders(springMessage.getHeaders(), rocketMessage);
        return rocketMessage;
    }

    protected byte[] convertPayload(org.springframework.messaging.Message<?> springMessage) {
        byte[] bytes;
        Object payload = springMessage.getPayload();
        if (payload instanceof byte[] bytePayload) {
            bytes = bytePayload;
        } else {
            Object serializedPayload = this.messagingConverter.fromMessage(springMessage, payload.getClass());
            if (serializedPayload == null) {
                String payloadType = payload.getClass().getName();
                Object contentType = springMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE);
                throw new MessageConversionException(String.format(
                        "Unable to convert payload with type='%s', contentType='%s', converter=[%s]"
                        , payloadType, contentType, this.messagingConverter));
            }
            try {
                bytes = serialize(serializedPayload);
            } catch (Exception e) {
                throw new MessageConversionException("convert to bytes failed.", e);
            }
        }
        return bytes;
    }

    private byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(stream).writeObject(object);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Could not serialize object of type: " + object.getClass(), e);
        }
        return stream.toByteArray();
    }

}
