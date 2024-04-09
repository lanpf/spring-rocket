package org.springframework.rocket.listener;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.ArrayList;
import java.util.Optional;

public abstract class RocketMessageListenerFactory {

    public static MessageListenerConcurrently createConcurrently(MessageListener listener, Integer delayLevelWhenNextConsume) {
        if (listener instanceof RocketMessageListener rocketMessageListener) {
            return (messages, context) -> {
                for (MessageExt message : messages) {
                    try {
                        rocketMessageListener.onMessage(message);
                    } catch (Exception e) {
                        Optional.ofNullable(delayLevelWhenNextConsume).ifPresent(context::setDelayLevelWhenNextConsume);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            };
        }
        else if (listener instanceof BatchRocketMessageListener batchRocketMessageListener) {
            return (messages, context) -> {
                try {
                    batchRocketMessageListener.onMessage(new ArrayList<>(messages));
                } catch (Exception e) {
                    Optional.ofNullable(delayLevelWhenNextConsume).ifPresent(context::setDelayLevelWhenNextConsume);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            };
        }
        throw new IllegalArgumentException("listener must be instance of RocketMessageListener or BatchRocketMessageListener");
    }


    public static MessageListenerOrderly createOrderly(MessageListener listener, Long suspendCurrentQueueTimeMillis) {
        if (listener instanceof RocketMessageListener rocketMessageListener) {
            return (messages, context) -> {
                for (MessageExt message : messages) {
                    try {
                        rocketMessageListener.onMessage(message);
                    } catch (Exception e) {
                        Optional.ofNullable(suspendCurrentQueueTimeMillis).ifPresent(context::setSuspendCurrentQueueTimeMillis);
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                }

                return ConsumeOrderlyStatus.SUCCESS;
            };
        }
        else if (listener instanceof BatchRocketMessageListener batchRocketMessageListener) {
            return (messages, context) -> {
                try {
                    batchRocketMessageListener.onMessage(new ArrayList<>(messages));
                } catch (Exception e) {
                    Optional.ofNullable(suspendCurrentQueueTimeMillis).ifPresent(context::setSuspendCurrentQueueTimeMillis);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }

                return ConsumeOrderlyStatus.SUCCESS;
            };
        }
        throw new IllegalArgumentException("listener must be instance of RocketMessageListener or BatchRocketMessageListener");
    }
}
