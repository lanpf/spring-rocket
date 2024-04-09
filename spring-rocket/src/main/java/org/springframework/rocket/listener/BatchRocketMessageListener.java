package org.springframework.rocket.listener;

import org.apache.rocketmq.common.message.Message;

import java.util.List;

@FunctionalInterface
public interface BatchRocketMessageListener extends MessageListener {

    void onMessage(List<Message> rocketMessages);
}
