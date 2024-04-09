package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RocketSendSimpleSimpleExtractionConsumer {

    public final static String KEY = "rocket-send-simple";
    public final static String GROUP_ID = "GID_ROCKET_SEND_SIMPLE_SIMPLE_EXTRACTION";


    @RocketListener(topic = KEY, groupId = GROUP_ID)
    @RocketHandler
    public void onMessage(PayloadReceive payload) {
        log.info("[{}]spring rocket receive simple extraction payload: {}", GROUP_ID, payload);
    }
}

