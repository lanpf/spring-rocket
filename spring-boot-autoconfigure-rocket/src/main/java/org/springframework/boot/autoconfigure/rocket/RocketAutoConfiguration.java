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
import org.springframework.rocket.client.DefaultRocketPullConsumerFactory;
import org.springframework.rocket.client.DefaultRocketPushConsumerFactory;
import org.springframework.rocket.client.RocketProducerFactory;
import org.springframework.rocket.client.RocketPullConsumerFactory;
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
            ObjectProvider<RocketTemplateCustomizer> customizers) {
        RocketTemplate rocketTemplate = new RocketTemplate(producerFactory);
        customizers.orderedStream().forEach(customizer -> customizer.customize(rocketTemplate));
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


    @Bean(name = "messageConverterRocketTemplateCustomizer")
    @ConditionalOnMissingBean(name = "messageConverterRocketTemplateCustomizer")
    public RocketTemplateCustomizer messageConverterRocketTemplateCustomizer(ObjectProvider<MessagingMessageConverter> messageConverter) {
        return rocketTemplate -> messageConverter.ifAvailable(rocketTemplate::setMessageConverter);
    }

    @Bean(name = "messageQueueSelectorRocketTemplateCustomizer")
    @ConditionalOnMissingBean(name = "messageQueueSelectorRocketTemplateCustomizer")
    public RocketTemplateCustomizer messageQueueSelectorRocketTemplateCustomizer(ObjectProvider<MessageQueueSelector> messageQueueSelector) {
        return rocketTemplate -> messageQueueSelector.ifAvailable(rocketTemplate::setMessageQueueSelector);
    }

    @Bean(name = "transactionExecutorRocketTemplateCustomizer")
    @ConditionalOnMissingBean(name = "transactionExecutorRocketTemplateCustomizer")
    public RocketTemplateCustomizer transactionExecutorRocketTemplateCustomizer(RocketProperties rocketProperties) {
        return rocketTemplate -> rocketTemplate.setTransactionExecutor(rocketProperties.getTemplate().getTransactionalExecutor().create());
    }

    @Bean(name = "pullConsumerFactoryRocketTemplateCustomizer")
    @ConditionalOnMissingBean(name = "pullConsumerFactoryRocketTemplateCustomizer")
    public RocketTemplateCustomizer pullConsumerFactoryRocketTemplateCustomizer(RocketProperties rocketProperties) {
        return rocketTemplate -> rocketTemplate.setPullConsumerFactory(new DefaultRocketPullConsumerFactory(rocketProperties.buildPullConsumerProperties()));
    }
}
