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
public class RocketSendDelayConsumer {

    public final static String KEY = "rocket-send-delay";
    public final static String GROUP_ID = "GID_ROCKET_SEND_DELAY";


    @RocketListener(topic = KEY, groupId = GROUP_ID)
    @RocketHandler
    public void onMessage(@Payload(required = false) PayloadReceive payload,
                          @Headers Map<String, Object> headers,
                          @Header(value = RocketHeaders.RECEIVED_TOPIC) String topic,
                          @Header(value = RocketHeaders.RECEIVED_QUEUE_ID) String queueId,
                          @Header(required = false, value = RocketHeaders.RECEIVED_DELIVER_TIME_MILLIS) String deliverTimeMillis,
                          @Header(required = false, value = RocketHeaders.RECEIVED_DELAY_MILLIS) String delayMillis,
                          @Header(required = false, value = RocketHeaders.RECEIVED_DELAY_SECONDS) String delaySeconds,
                          @Header(required = false, value = RocketHeaders.RECEIVED_DELAY_LEVEL) String delayLevel) {
        log.info("[{}]spring rocket receive {} payload: {}, headers: {}, queueId: {}, delay[deliverTimeMillis: {}, delayMillis: {}, delaySeconds: {}, delayLevel: {}]",
                GROUP_ID, topic, payload, headers, queueId, deliverTimeMillis, delayMillis, delaySeconds, delayLevel);
    }
}

