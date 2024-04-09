package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RocketSendBatchConsumer {

    public final static String KEY = "rocket-send-batch";
    public final static String GROUP_ID = "GID_ROCKET_SEND_BATCH";


    @RocketListener(topic = KEY, groupId = GROUP_ID, batch = true, properties = {"consumeBatchSize:3"})
    @RocketHandler
    public void onMessage(@Payload List<PayloadReceive> payloads, @Headers Map<String, Object> headers) {
        log.info("[{}]spring rocket receive [{}] payloads: {}, headers: {}", GROUP_ID, payloads.size(), payloads, headers);
    }
}

