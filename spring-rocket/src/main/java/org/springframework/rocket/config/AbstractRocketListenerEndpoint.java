package org.springframework.rocket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.listener.MessageListenerContainer;
import org.springframework.rocket.listener.RocketMessageListenerContainer;
import org.springframework.rocket.listener.adapter.AbstractRocketMessageListenerAdapter;
import org.springframework.rocket.support.MessageConverter;
import org.springframework.util.Assert;

import java.util.Properties;

@Getter
@Setter
public abstract class AbstractRocketListenerEndpoint implements RocketListenerEndpoint, BeanFactoryAware, InitializingBean {

    private String id;
    private String groupId;
    private String topic;
    private String filterExpressionType;
    private String filterExpression;

    private Properties consumerProperties;

    private Boolean batchListener;

    private Boolean concurrency;

    private Boolean autoStartup;

    private RocketTemplate replyTemplate;

    private BeanFactory beanFactory;

    private BeanExpressionResolver resolver;

    private BeanExpressionContext expressionContext;

    private BeanResolver beanResolver;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory configurable) {
            this.resolver = configurable.getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext(configurable, null);
        }
        this.beanResolver = new BeanFactoryResolver(beanFactory);
    }

    @Override
    public void afterPropertiesSet() {

    }

    @Override
    public void setupListenerContainer(RocketMessageListenerContainer listenerContainer, MessageConverter messageConverter) {
        setupMessageListener(listenerContainer, messageConverter);
    }

    private void setupMessageListener(RocketMessageListenerContainer container, MessageConverter messageConverter) {
        AbstractRocketMessageListenerAdapter messageListener = createMessageListener(container, messageConverter);
        Assert.state(messageListener != null, String.format("Endpoint [%s] must provide a non null message listener", this));
        container.setupMessageListener(messageListener);
    }


    protected abstract AbstractRocketMessageListenerAdapter createMessageListener(MessageListenerContainer container, MessageConverter messageConverter);




}
