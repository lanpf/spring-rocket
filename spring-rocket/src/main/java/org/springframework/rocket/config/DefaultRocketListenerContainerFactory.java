package org.springframework.rocket.config;

import lombok.Setter;
import org.springframework.rocket.client.RocketPushConsumerFactory;
import org.springframework.rocket.listener.ContainerProperties;
import org.springframework.rocket.listener.DefaultRocketMessageListenerContainer;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Setter
public class DefaultRocketListenerContainerFactory
        extends AbstractRocketListenerContainerFactory<DefaultRocketMessageListenerContainer> {

    private Boolean concurrency;

    public DefaultRocketListenerContainerFactory(RocketPushConsumerFactory consumerFactory, ContainerProperties containerProperties) {
        super(consumerFactory, containerProperties);
    }

    public DefaultRocketListenerContainerFactory(RocketPushConsumerFactory consumerFactory) {
        this(consumerFactory, new ContainerProperties());
    }


    @Override
    protected DefaultRocketMessageListenerContainer createContainerInstance(RocketListenerEndpoint endpoint) {
        ContainerProperties overrideContainerProperties = new ContainerProperties();

        JavaUtils instance = JavaUtils.INSTANCE;
        instance
                .acceptIfHasText(endpoint.getTopic(), overrideContainerProperties::setTopic)
                .acceptIfHasText(endpoint.getFilterExpressionType(), overrideContainerProperties::setFilterExpressionType)
                .acceptIfHasText(endpoint.getFilterExpression(), overrideContainerProperties::setFilterExpression)
                .acceptIfNotNull(endpoint.getBatchListener(), overrideContainerProperties::setBatchListener);
        if (!ObjectUtils.isEmpty(endpoint.getConsumerProperties())) {
            overrideContainerProperties.setRocketConsumerProperties(endpoint.getConsumerProperties());
        }


        if (!StringUtils.hasText(overrideContainerProperties.getGroupId())) {
            instance.acceptIfHasText(this.getContainerProperties().getGroupId(), overrideContainerProperties::setGroupId);
        }
        if (!StringUtils.hasText(overrideContainerProperties.getTopic())) {
            instance.acceptIfHasText(this.getContainerProperties().getTopic(), overrideContainerProperties::setTopic);
        }
        if (!StringUtils.hasText(overrideContainerProperties.getFilterExpressionType())) {
            instance.acceptIfHasText(this.getContainerProperties().getFilterExpressionType(), overrideContainerProperties::setFilterExpressionType);
        }
        if (!StringUtils.hasText(overrideContainerProperties.getFilterExpression())) {
            instance.acceptIfHasText(this.getContainerProperties().getFilterExpression(), overrideContainerProperties::setFilterExpression);
        }
        if (overrideContainerProperties.getBatchListener() == null) {
            instance.acceptIfNotNull(this.getContainerProperties().getBatchListener(), overrideContainerProperties::setBatchListener);
        }
        if (overrideContainerProperties.getMessageListener() == null) {
            instance.acceptIfNotNull(this.getContainerProperties().getMessageListener(), overrideContainerProperties::setMessageListener);
        }
        if (overrideContainerProperties.getRocketConsumerProperties() != null && this.getContainerProperties().getRocketConsumerProperties() != null) {
            this.getContainerProperties().getRocketConsumerProperties().forEach((k, v) -> overrideContainerProperties.getRocketConsumerProperties().putIfAbsent(k, v));
        }

        return new DefaultRocketMessageListenerContainer(this.getConsumerFactory(), overrideContainerProperties);
    }

    @Override
    protected void initializeContainer(DefaultRocketMessageListenerContainer instance, RocketListenerEndpoint endpoint) {
        super.initializeContainer(instance, endpoint);
        if (endpoint.getConcurrency() != null) {
            instance.setConcurrency(endpoint.getConcurrency());
        }
        else if (this.concurrency != null) {
            instance.setConcurrency(this.concurrency);
        }
    }
}
