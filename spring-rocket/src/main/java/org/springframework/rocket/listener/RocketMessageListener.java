package org.springframework.rocket.listener;

import org.apache.rocketmq.common.message.Message;

@FunctionalInterface
public interface RocketMessageListener extends MessageListener {

    void onMessage(Message rocketMessage);
}
