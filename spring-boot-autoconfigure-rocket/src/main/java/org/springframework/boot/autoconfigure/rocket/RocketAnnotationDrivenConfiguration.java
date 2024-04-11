package org.springframework.boot.autoconfigure.rocket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rocket.annotation.EnableRocket;
import org.springframework.rocket.annotation.RocketListenerAnnotationBeanPostProcessor;
import org.springframework.rocket.client.RocketPushConsumerFactory;
import org.springframework.rocket.config.DefaultRocketListenerContainerFactory;
import org.springframework.rocket.config.RocketSupportBeanNames;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.support.converter.MessagingMessageConverter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRocket.class)
public class RocketAnnotationDrivenConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DefaultRocketListenerContainerFactoryConfigurer rocketListenerContainerFactoryConfigurer(
            ObjectProvider<RocketProperties> rocketProperties,
            ObjectProvider<MessagingMessageConverter> messageConverter,
            ObjectProvider<RocketTemplate> replyTemplate) {
        DefaultRocketListenerContainerFactoryConfigurer configurer = new DefaultRocketListenerContainerFactoryConfigurer(rocketProperties.getIfUnique());
        configurer.setMessageConverter(messageConverter.getIfUnique());
        configurer.setReplyTemplate(replyTemplate.getIfUnique());
        return configurer;
    }

    @Bean(name = RocketListenerAnnotationBeanPostProcessor.DEFAULT_ROCKET_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    @ConditionalOnMissingBean(name = RocketListenerAnnotationBeanPostProcessor.DEFAULT_ROCKET_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    DefaultRocketListenerContainerFactory rocketListenerContainerFactory(
            DefaultRocketListenerContainerFactoryConfigurer configurer, ObjectProvider<RocketPushConsumerFactory> consumerFactory) {
        DefaultRocketListenerContainerFactory containerFactory = new DefaultRocketListenerContainerFactory(consumerFactory.getIfAvailable());
        configurer.configure(containerFactory);
        return containerFactory;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRocket
    @ConditionalOnMissingBean(name = RocketSupportBeanNames.ROCKET_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
    static class EnableRocketConfiguration {
    }
}
