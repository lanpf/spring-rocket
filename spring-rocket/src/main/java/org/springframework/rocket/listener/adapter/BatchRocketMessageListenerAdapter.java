package org.springframework.rocket.listener.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageBatch;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rocket.listener.BatchRocketMessageListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BatchRocketMessageListenerAdapter extends AbstractRocketMessageListenerAdapter implements BatchRocketMessageListener {

    // errorHandler

    public BatchRocketMessageListenerAdapter(Object bean, Method method) {
        super(bean, method);
    }


    @Override
    public void onMessage(List<Message> rocketMessages) {
        Object data = null;
        org.springframework.messaging.Message<?> springMessage = null;

        // List<Message> rocketMessages
        if (isRocketMessageList() && !isHeaderFound()) {
            data = rocketMessages;
        }
        // List<Message> rocketMessages, @Headers Map<String, Object> headers
        else if (isRocketMessageList()) {
            List<org.springframework.messaging.Message<?>> springMessages = toSpringMessages(rocketMessages);
            Map<String, List<Object>> aggregatedHeaders = withAggregatedHeaders(springMessages);
            springMessage = MessageBuilder.withPayload(new ArrayList<>(rocketMessages)).copyHeaders(aggregatedHeaders).build();
        }
        // List<Message<?>> springMessages
        else if (isSpringMessageList() && !isHeaderFound()) {
            List<org.springframework.messaging.Message<?>> springMessages = toSpringMessages(rocketMessages);
            springMessage = MessageBuilder.withPayload(springMessages).build();
        }
        // List<Message<?>> springMessages, @Headers Map<String, Object> headers
        else if (isSpringMessageList()) {
            List<org.springframework.messaging.Message<?>> springMessages = toSpringMessages(rocketMessages);
            Map<String, List<Object>> aggregatedHeaders = withAggregatedHeaders(springMessages);
            springMessage = MessageBuilder.withPayload(springMessages).copyHeaders(aggregatedHeaders).build();
        }
        // List<PayloadReceive> payloads
        else if (this.isSimpleExtraction()) {
            List<org.springframework.messaging.Message<?>> springMessages = toSpringMessages(rocketMessages);
            data = withPayloads(springMessages);
        }
        // List<PayloadReceive> payloads, @Headers Map<String, Object> headers
        else if (isHeaderFound()) {
            List<org.springframework.messaging.Message<?>> springMessages = toSpringMessages(rocketMessages);
            Map<String, List<Object>> aggregatedHeaders = withAggregatedHeaders(springMessages);
            springMessage = MessageBuilder.withPayload(withPayloads(springMessages)).copyHeaders(aggregatedHeaders).build();
        }

        // MessageBatch
        if (isRocketMessages()) {
            data = MessageBatch.generateFromList(rocketMessages);
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing [{}]", springMessage);
        }
        invokeHandler(springMessage, data);
//        Object result = invokeHandler(springMessage, data);
//        if (result != null) {
//            handleResult(result, rocketMessages, springMessage);
//        }
    }

    private Map<String, List<Object>> withAggregatedHeaders(List<org.springframework.messaging.Message<?>> springMessages) {
        Map<String, List<Object>> aggregatedHeaders = new HashMap<>();
        for (org.springframework.messaging.Message<?> springMessage : springMessages) {
            springMessage.getHeaders()
                    .forEach((k, v) -> aggregatedHeaders.computeIfAbsent(k, key -> new ArrayList<>()).add(v));
        }
        return aggregatedHeaders;
    }

    private List<Object> withPayloads(List<org.springframework.messaging.Message<?>> springMessages) {
        List<Object> payloads = new ArrayList<>(springMessages.size());
        for (org.springframework.messaging.Message<?> springMessage : springMessages) {
            payloads.add(springMessage.getPayload());
        }
        return payloads;
    }

    protected List<org.springframework.messaging.Message<?>> toSpringMessages(List<Message> rocketMessages) {
        List<org.springframework.messaging.Message<?>> springMessages = new ArrayList<>(rocketMessages.size());
        for (Message rocketMessage : rocketMessages) {
            springMessages.add(toMessagingMessage(rocketMessage));
        }
        return springMessages;
    }

}