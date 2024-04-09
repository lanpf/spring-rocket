package org.springframework.rocket.test;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.rocket.support.converter.DefaultMessagingMessageConverter;
import org.springframework.rocket.support.converter.MessagingMessageConverter;
import org.springframework.rocket.test.util.MessageConverterFactory;

@SpringBootApplication
public class SpringRocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringRocketApplication.class, args);
    }


    @Bean
    @ConditionalOnMissingBean
    public MessagingMessageConverter rocketMessagingMessageConverter(ObjectProvider<ObjectMapper> objectMapper) {
        MessageConverter messageConverter = MessageConverterFactory.create();
        configureJackson(messageConverter, objectMapper);
        if (messageConverter instanceof CompositeMessageConverter compositeMessageConverter) {
            compositeMessageConverter.getConverters().forEach(converter -> configureJackson(converter, objectMapper));
        }
        DefaultMessagingMessageConverter rocketMessagingMessageConverter = new DefaultMessagingMessageConverter();
        rocketMessagingMessageConverter.setMessagingConverter(messageConverter);
        return rocketMessagingMessageConverter;
    }


    private void configureJackson(MessageConverter messageConverter, ObjectProvider<ObjectMapper> objectMapper) {
        if (messageConverter instanceof MappingJackson2MessageConverter jacksonMessageConverter) {
            objectMapper.ifUnique((jacksonMessageConverter)::setObjectMapper);
        }
    }
}
