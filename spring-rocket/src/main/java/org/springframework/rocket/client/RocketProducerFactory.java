package org.springframework.rocket.client;

import org.apache.rocketmq.client.producer.MQProducer;

public interface RocketProducerFactory extends RocketClientFactory<MQProducer> {
}
