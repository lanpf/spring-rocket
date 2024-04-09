package org.springframework.rocket.listener;

import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.common.filter.ExpressionType;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.rocket.client.PushConsumerProperties;
import org.springframework.rocket.client.RocketPushConsumerFactory;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultRocketMessageListenerContainer extends AbstractRocketMessageListenerContainer {

    private final RocketPushConsumerFactory consumerFactory;
    private final Lock lockOnPause = new ReentrantLock();
    private final Condition pausedCondition = this.lockOnPause.newCondition();
    private MQPushConsumer consumer;
    @Setter
    private Boolean concurrency;

    public DefaultRocketMessageListenerContainer(RocketPushConsumerFactory consumerFactory, ContainerProperties containerProperties) {
        super(containerProperties);
        this.consumerFactory = consumerFactory;
    }


    @SneakyThrows
    @Override
    protected void doStart() {
        Map<String, Object> consumerProperties = PropertiesUtils.asMap(this.containerProperties.getRocketConsumerProperties());
        configure(consumerProperties);
        Assert.state(this.consumerFactory != null, "No 'consumerFactory' set");
        this.consumer = this.consumerFactory.create(this.getGroupId(), consumerProperties);
        Assert.state(this.consumer != null, "Unable to create a consumer");

        if (this.consumer instanceof DefaultMQPushConsumer defaultMQPushConsumer
                && !Boolean.TRUE.equals(this.concurrency)
                && MessageModel.BROADCASTING.equals(defaultMQPushConsumer.getMessageModel())) {
            throw new IllegalStateException("messageModel BROADCASTING does not support ORDERLY message");
        }

        subscribeTopic();
        registerMessageListener(consumerProperties);

        this.consumer.start();
        setRunning(true);
    }

    @Override
    protected void doStop() {
        setRunning(false);
        if (this.consumer != null) {
            this.consumer.shutdown();
        }
    }


    @Override
    protected void doPause() {
        setPaused(true);
        if (this.consumer != null) {
            this.consumer.suspend();
        }
    }

    @Override
    protected void doResume() {
        if (this.consumer != null) {
            this.consumer.resume();
        }
        setPaused(false);
        this.lockOnPause.lock();
        try {
            // signal the lock's condition to continue.
            this.pausedCondition.signal();
        }
        finally {
            this.lockOnPause.unlock();
        }
    }

    @SneakyThrows
    private void subscribeTopic() {
        String topic = this.containerProperties.getTopic();
        String filterExpressionType = this.containerProperties.getFilterExpression();

        if (ExpressionType.isTagType(this.containerProperties.getFilterExpressionType())) {
            this.consumer.subscribe(topic, filterExpressionType);
        } else {
            this.consumer.subscribe(topic, MessageSelector.bySql(filterExpressionType));
        }
    }

    private void registerMessageListener(Map<String, Object> consumerProperties) {
        MessageListener messageListener = (MessageListener) containerProperties.getMessageListener();
        if (Boolean.TRUE.equals(this.concurrency)) {
            this.consumer.registerMessageListener(RocketMessageListenerFactory.createConcurrently(messageListener,
                    PropertiesUtils.extractAsInteger(consumerProperties, PushConsumerProperties.RETRY_DELAY_LEVEL)));
        } else {
            this.consumer.registerMessageListener(RocketMessageListenerFactory.createOrderly(messageListener,
                    PropertiesUtils.extractAsLong(consumerProperties, PushConsumerProperties.SUSPEND_CURRENT_QUEUE_TIME_MILLIS)));
        }
    }


    private void configure(Map<String, Object> consumerProperties) {
        // consumer
    }
}
