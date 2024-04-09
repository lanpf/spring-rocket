package org.springframework.rocket.test.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.rocket.annotation.RocketHandler;
import org.springframework.rocket.annotation.RocketListener;
import org.springframework.rocket.test.dto.PayloadReceive;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RocketSendBatchMessagingConsumer {

    public final static String KEY = "rocket-send-batch";
    public final static String GROUP_ID = "GID_ROCKET_SEND_BATCH_MESSAGING";


    @RocketListener(topic = KEY, groupId = GROUP_ID, batch = true, properties = {"consumeBatchSize:3"})
    @RocketHandler
    public void onMessage(List<Message<PayloadReceive>> messages) {
        log.info("[{}]spring rocket receive [{}] messaging messages: {}", GROUP_ID, messages.size(), messages);
    }
}

