package org.springframework.rocket.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.rocket.client.RocketPushConsumerFactory;
import org.springframework.rocket.listener.AbstractRocketMessageListenerContainer;
import org.springframework.rocket.listener.ContainerProperties;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.rocket.support.MessageConverter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractRocketListenerContainerFactory<C extends AbstractRocketMessageListenerContainer>
        implements RocketListenerContainerFactory, ApplicationEventPublisherAware, ApplicationContextAware {

    private final RocketPushConsumerFactory consumerFactory;

    private final ContainerProperties containerProperties;

    private Boolean autoStartup;

    private Integer phase;

    private MessageConverter messageConverter;

    private ApplicationEventPublisher applicationEventPublisher;

    private ApplicationContext applicationContext;

    private ContainerCustomizer<C> containerCustomizer;


    @Override
    public C createListenerContainer(RocketListenerEndpoint endpoint) {
        C instance = createContainerInstance(endpoint);
        JavaUtils.INSTANCE.acceptIfHasText(endpoint.getId(), instance::setBeanName);
        if (endpoint instanceof AbstractRocketListenerEndpoint abstractEndpoint) {
            configureEndpoint(abstractEndpoint);
        }

        endpoint.setupListenerContainer(instance, this.messageConverter);
        initializeContainer(instance, endpoint);
        customizeContainer(instance);
        return instance;
    }

    protected abstract C createContainerInstance(RocketListenerEndpoint endpoint);

    private void configureEndpoint(AbstractRocketListenerEndpoint abstractEndpoint) {
        // empty
    }

    protected void initializeContainer(C instance, RocketListenerEndpoint endpoint) {
        ContainerProperties instanceProperties = instance.getContainerProperties();

        if (!StringUtils.hasText(instanceProperties.getTopic())) {
            JavaUtils.INSTANCE.acceptIfHasText(this.containerProperties.getTopic(),
                    instanceProperties::setTopic);
        }

        if (!StringUtils.hasText(instanceProperties.getFilterExpressionType())) {
            JavaUtils.INSTANCE.acceptIfHasText(this.containerProperties.getFilterExpressionType(),
                    instanceProperties::setFilterExpressionType);
        }

        if (!StringUtils.hasText(instanceProperties.getFilterExpression())) {
            JavaUtils.INSTANCE.acceptIfHasText(this.containerProperties.getFilterExpression(),
                    instanceProperties::setFilterExpression);
        }

        if (endpoint.getAutoStartup() != null) {
            instance.setAutoStartup(endpoint.getAutoStartup());
        }
        else if (this.autoStartup != null) {
            instance.setAutoStartup(this.autoStartup);
        }


        JavaUtils.INSTANCE
                .acceptIfNotNull(this.phase, instance::setPhase)
                .acceptIfNotNull(this.applicationContext, instance::setApplicationContext)
                .acceptIfNotNull(this.applicationEventPublisher, instance::setApplicationEventPublisher)
                .acceptIfHasText(endpoint.getGroupId(), instance.getContainerProperties()::setGroupId)
                .acceptIfNotNull(endpoint.getConsumerProperties(),
                        instance.getContainerProperties()::setRocketConsumerProperties);
        // Update container properties if there are relevant direct consumer properties
        instanceProperties.updateContainerProperties();
    }

    protected void customizeContainer(C container) {
        if (this.containerCustomizer != null) {
            this.containerCustomizer.configure(container);
        }
    }
}
