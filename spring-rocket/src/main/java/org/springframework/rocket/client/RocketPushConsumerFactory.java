package org.springframework.rocket.client;

import org.apache.rocketmq.client.consumer.MQPushConsumer;

public interface RocketPushConsumerFactory extends RocketConsumerFactory<MQPushConsumer> {
}
