package org.springframework.rocket.client;

import org.apache.rocketmq.client.consumer.LitePullConsumer;

public interface RocketPullConsumerFactory extends RocketConsumerFactory<LitePullConsumer> {
}
