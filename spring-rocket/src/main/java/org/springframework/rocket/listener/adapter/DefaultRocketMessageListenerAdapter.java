package org.springframework.rocket.listener.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.springframework.rocket.listener.RocketMessageListener;

import java.lang.reflect.Method;

@Slf4j
public class DefaultRocketMessageListenerAdapter extends AbstractRocketMessageListenerAdapter implements RocketMessageListener {

    // errorHandler

    public DefaultRocketMessageListenerAdapter(Object bean, Method method) {
        super(bean, method);
    }

    @Override
    public void onMessage(Message rocketMessage) {
        Object data = rocketMessage;
        org.springframework.messaging.Message<?> springMessage = null;
        if (isSpringMessage() || isHeaderFound()) {
            springMessage = toMessagingMessage(rocketMessage);
        }
        else if (isSimpleExtraction()) {
            data = toMessagingMessage(rocketMessage).getPayload();
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing [{}]", springMessage);
        }
        Object result = invokeHandler(springMessage, data);
        if (result != null) {
            handleResult(result, rocketMessage, springMessage);
        }
    }
}