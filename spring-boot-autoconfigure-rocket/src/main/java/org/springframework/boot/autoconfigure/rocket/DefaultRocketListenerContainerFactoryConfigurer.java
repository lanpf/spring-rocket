package org.springframework.boot.autoconfigure.rocket;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.rocket.config.DefaultRocketListenerContainerFactory;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.listener.ContainerProperties;
import org.springframework.rocket.support.MessageConverter;

@Setter
@RequiredArgsConstructor
public class DefaultRocketListenerContainerFactoryConfigurer {

    private final RocketProperties rocketProperties;

    private MessageConverter messageConverter;


    public void configure(DefaultRocketListenerContainerFactory containerFactory) {
        configureListenerContainerFactory(containerFactory);
        configureContainer(containerFactory.getContainerProperties());
    }

    private void configureListenerContainerFactory(DefaultRocketListenerContainerFactory containerFactory) {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        RocketProperties.Listener properties = this.rocketProperties.getListener();
        map.from(properties::getConcurrency).to(containerFactory::setConcurrency);
        map.from(this.messageConverter).to(containerFactory::setMessageConverter);
    }

    private void configureContainer(ContainerProperties containerProperties) {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(this.rocketProperties::buildPushConsumerProperties).to(containerProperties::setRocketConsumerProperties);
    }
}
