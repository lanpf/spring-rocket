package org.springframework.rocket.test.util;

import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class MessageConverterFactory {

    public static final String DEFAULT_MESSAGE_CONVERTER_NAME = "customMessagingMessageConverter";

    private static final boolean JACKSON_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)
            && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", null);
    private static final boolean FASTJSON_PRESENT = ClassUtils.isPresent("com.alibaba.fastjson.JSON", null)
            && ClassUtils.isPresent("com.alibaba.fastjson.support.config.FastJsonConfig", null);

    public static MessageConverter create() {
        List<MessageConverter> messageConverters = new ArrayList<>();
        if (JACKSON_PRESENT) {
            messageConverters.add(new MappingJackson2MessageConverter());
        }
        if (FASTJSON_PRESENT) {
            try {
                messageConverters.add((MessageConverter) ClassUtils.forName("com.alibaba.fastjson.support.spring.messaging.MappingFastJsonMessageConverter", ClassUtils.getDefaultClassLoader())
                        .getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException ignored) {
            }
        }

        ByteArrayMessageConverter byteArrayMessageConverter = new ByteArrayMessageConverter();
        byteArrayMessageConverter.setContentTypeResolver(null);
        messageConverters.add(byteArrayMessageConverter);
        messageConverters.add(new StringMessageConverter());

        return new CompositeMessageConverter(messageConverters);
    }
}
