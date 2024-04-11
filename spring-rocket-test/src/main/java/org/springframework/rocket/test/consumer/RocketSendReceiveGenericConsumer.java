package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.rocket.test.dto.Result;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RocketSendReceiveGenericConsumer {

    public final static String KEY = "rocket-send-receive-generic";
    public final static String GROUP_ID = "GID_ROCKET_SEND_RECEIVE_GENERIC";


    @RocketListener(topic = KEY, concurrency = "false", groupId = GROUP_ID)
    @RocketHandler
    public Result<PayloadReceive> onMessage(@Payload PayloadReceive payload,
                                            @Headers Map<String, Object> headers,
                                            @Header(value = RocketHeaders.RECEIVED_TOPIC) String topic,
                                            @Header(value = RocketHeaders.RECEIVED_QUEUE_ID) String queueId) {
        log.info("[{}]spring rocket receive {} payload: {}, headers: {}, queueId: {}", GROUP_ID, topic, payload, headers, queueId);

        PayloadReceive reply = new PayloadReceive();
        reply.setId(String.format("response for [%s]", payload.getId()));
        reply.setTimestamp(System.currentTimeMillis());

        return Result.of(reply);
    }
}