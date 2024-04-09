package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RocketSendSimpleMessagingConsumer {

    public final static String KEY = "rocket-send-simple";
    public final static String GROUP_ID = "GID_ROCKET_SEND_SIMPLE_MESSAGING";


    @RocketListener(topic = KEY, groupId = GROUP_ID)
    @RocketHandler
    public void onMessage(Message<PayloadReceive> message) {
        log.info("[{}]spring rocket receive {} messaging message: {}", GROUP_ID, message.getHeaders().get(RocketHeaders.RECEIVED_TOPIC), message);
    }
}

