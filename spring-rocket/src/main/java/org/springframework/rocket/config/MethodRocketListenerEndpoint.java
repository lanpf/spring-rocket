package org.springframework.rocket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.BeanResolver;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.rocket.listener.MessageListenerContainer;
import org.springframework.rocket.listener.adapter.AbstractRocketMessageListenerAdapter;
import org.springframework.rocket.listener.adapter.BatchRocketMessageListenerAdapter;
import org.springframework.rocket.listener.adapter.DefaultRocketMessageListenerAdapter;
import org.springframework.rocket.listener.adapter.HandlerAdapter;
import org.springframework.rocket.support.MessageConverter;
import org.springframework.rocket.support.converter.MessagingMessageConverter;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

@Setter
@Getter
public class MethodRocketListenerEndpoint extends AbstractRocketListenerEndpoint {

    private Object bean;
    private Method method;
    private MessageHandlerMethodFactory messageHandlerMethodFactory;
    private SmartMessageConverter messagingConverter;

    @Override
    protected AbstractRocketMessageListenerAdapter createMessageListener(MessageListenerContainer container, MessageConverter messageConverter) {
        Assert.state(this.messageHandlerMethodFactory != null,
                "Could not create message listener - MessageHandlerMethodFactory not set");

        AbstractRocketMessageListenerAdapter messageListener = createMessageListenerInstance(messageConverter);
        HandlerAdapter handlerMethod = configureListenerAdapter();
        messageListener.setHandlerMethod(handlerMethod);

        return messageListener;
    }

    /**
     * Create a {@link HandlerAdapter} for this listener adapter.
     * @return the handler adapter.
     */
    protected HandlerAdapter configureListenerAdapter() {
        InvocableHandlerMethod invocableHandlerMethod = this.messageHandlerMethodFactory.createInvocableHandlerMethod(this.bean, this.method);
        return new HandlerAdapter(invocableHandlerMethod);
    }

    /**
     * Create an empty {@link AbstractRocketMessageListenerAdapter} instance.
     * @param messageConverter the converter (may be null).
     * @return the {@link AbstractRocketMessageListenerAdapter} instance.
     */
    protected AbstractRocketMessageListenerAdapter createMessageListenerInstance(MessageConverter messageConverter) {

        AbstractRocketMessageListenerAdapter listener;
        if (Boolean.TRUE.equals(getBatchListener())) {
            listener = new BatchRocketMessageListenerAdapter(this.bean, this.method);
        } else {
            listener = new DefaultRocketMessageListenerAdapter(this.bean, this.method);
        }
        if (messageConverter instanceof MessagingMessageConverter messagingMessageConverter) {
            listener.setMessageConverter(messagingMessageConverter);
        }
        if (this.messagingConverter != null) {
            listener.setMessagingConverter(this.messagingConverter);
        }
        BeanResolver resolver = getBeanResolver();
        if (resolver != null) {
            listener.setBeanResolver(resolver);
        }
        return listener;
    }

}
