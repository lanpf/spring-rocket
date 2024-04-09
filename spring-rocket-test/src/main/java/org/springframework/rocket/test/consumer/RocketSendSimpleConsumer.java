package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RocketSendSimpleConsumer {

    public final static String KEY = "rocket-send-simple";
    public final static String GROUP_ID = "GID_ROCKET_SEND_SIMPLE";


    @RocketListener(topic = KEY, groupId = GROUP_ID)
    @RocketHandler
    public void onMessage(@Payload(required = false) PayloadReceive payload,
                          @Headers Map<String, Object> headers,
                          @Header(value = RocketHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = RocketHeaders.RECEIVED_QUEUE_ID) String queueId) {
        log.info("[{}]spring rocket receive {} payload: {}, headers: {}, queueId: {}", GROUP_ID, topic, payload, headers, queueId);
    }
}

