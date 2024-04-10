package org.springframework.boot.autoconfigure.rocket;

import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.rocket.client.DefaultRocketProducerFactory;
import org.springframework.rocket.client.DefaultRocketPushConsumerFactory;
import org.springframework.rocket.client.RocketProducerFactory;
import org.springframework.rocket.client.RocketPushConsumerFactory;
import org.springframework.rocket.config.RocketSupportBeanNames;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.support.converter.MessagingMessageConverter;

@AutoConfiguration
@ConditionalOnClass(RocketTemplate.class)
@EnableConfigurationProperties(RocketProperties.class)
@Import({ RocketAnnotationDrivenConfiguration.class })
public class RocketAutoConfiguration {

    @Bean(name = RocketSupportBeanNames.DEFAULT_ROCKET_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = RocketSupportBeanNames.DEFAULT_ROCKET_TEMPLATE_BEAN_NAME)
    public RocketTemplate rocketTemplate(
            RocketProducerFactory producerFactory,
            ObjectProvider<MessagingMessageConverter> messageConverter,
            ObjectProvider<MessageQueueSelector> messageQueueSelector) {
        RocketTemplate rocketTemplate = new RocketTemplate(producerFactory);
        messageConverter.ifAvailable(rocketTemplate::setMessageConverter);
        messageQueueSelector.ifAvailable(rocketTemplate::setMessageQueueSelector);
        return rocketTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(RocketProducerFactory.class)
    public RocketProducerFactory rocketProducerFactory(RocketProperties rocketProperties) {
        return new DefaultRocketProducerFactory(rocketProperties.buildProducerProperties());
    }

    @Bean
    @ConditionalOnMissingBean(RocketPushConsumerFactory.class)
    public RocketPushConsumerFactory rocketPushConsumerFactory(RocketProperties rocketProperties) {
        return new DefaultRocketPushConsumerFactory(rocketProperties.buildPushConsumerProperties());
    }
}
