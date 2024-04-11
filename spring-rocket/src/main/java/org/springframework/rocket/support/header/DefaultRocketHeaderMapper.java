package org.springframework.rocket.support.header;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.messaging.MessageHeaders;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.rocket.support.RocketHeaderUtils;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Objects;

public class DefaultRocketHeaderMapper implements RocketHeaderMapper {
    @Override
    public void fromSpringHeaders(Map<String, Object> springHeaders, Message rocketMessage) {
        Objects.requireNonNull(rocketMessage, "rocketMessage must not be null");
        if (ObjectUtils.isEmpty(springHeaders)) {
            return;
        }
        RocketHeaderUtils.TAGS_HEADER_GET.accept(rocketMessage, springHeaders);
        RocketHeaderUtils.KEYS_HEADER_GET.accept(rocketMessage, springHeaders);
        RocketHeaderUtils.FLAG_HEADER_GET.accept(rocketMessage, springHeaders);
        RocketHeaderUtils.WAIT_STORE_MSG_OK_HEADER_GET.accept(rocketMessage, springHeaders);

        RocketHeaderUtils.DELAY_HEADER_GET.accept(rocketMessage, springHeaders);

        springHeaders.entrySet().stream()
                .filter(entry -> customize(entry.getKey()))
                .forEach(entry -> MessageAccessor.putProperty(rocketMessage, entry.getKey(), String.valueOf(entry.getValue())));
    }

    @Override
    public void toSpringHeaders(Message rocketMessage, Map<String, Object> springHeaders) {
        Objects.requireNonNull(rocketMessage, "rocketMessage must not be null");
        springHeaders.put(RocketHeaders.RECEIVED_TOPIC, rocketMessage.getTopic());
        JavaUtils.INSTANCE
                .acceptIfHasText(rocketMessage.getTags(), value -> springHeaders.put(RocketHeaders.RECEIVED_TAGS, value))
                .acceptIfHasText(rocketMessage.getKeys(), value -> springHeaders.put(RocketHeaders.RECEIVED_KEYS, value))
                .acceptIfNotNull(rocketMessage.getFlag(), value -> springHeaders.put(RocketHeaders.RECEIVED_FLAG, value))
                .acceptIfNotNull(rocketMessage.isWaitStoreMsgOK(), value -> springHeaders.put(RocketHeaders.RECEIVED_WAIT_STORE_MSG_OK, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELIVER_MS), value -> springHeaders.put(RocketHeaders.RECEIVED_DELIVER_TIME_MILLIS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELAY_MS), value -> springHeaders.put(RocketHeaders.RECEIVED_DELAY_MILLIS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELAY_SEC), value -> springHeaders.put(RocketHeaders.RECEIVED_DELAY_SECONDS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL), value -> springHeaders.put(RocketHeaders.RECEIVED_DELAY_LEVEL, value));

        if (rocketMessage instanceof MessageExt messageExt) {
            JavaUtils.INSTANCE
                    .acceptIfHasText(messageExt.getMsgId(), value -> springHeaders.put(RocketHeaders.RECEIVED_MESSAGE_ID, value))
                    .acceptIfNotNull(messageExt.getQueueId(), value -> springHeaders.put(RocketHeaders.RECEIVED_QUEUE_ID, value))
                    .acceptIfNotNull(messageExt.getQueueOffset(), value -> springHeaders.put(RocketHeaders.RECEIVED_QUEUE_OFFSET, value))
                    .acceptIfNotNull(messageExt.getBornTimestamp(), value -> springHeaders.put(RocketHeaders.RECEIVED_BORN_TIMESTAMP, value))
                    .acceptIfHasText(messageExt.getBornHostString(), value -> springHeaders.put(RocketHeaders.RECEIVED_BORN_HOST, value))
                    .acceptIfNotNull(messageExt.getSysFlag(), value -> springHeaders.put(RocketHeaders.RECEIVED_SYS_FLAG, value));
        }
        if (!ObjectUtils.isEmpty(rocketMessage.getProperties())) {
            rocketMessage.getProperties().entrySet().stream()
                    .filter(entry -> !MessageHeaders.ID.equals(entry.getKey())
                            && !MessageHeaders.TIMESTAMP.equals(entry.getKey())
                            && customize(entry.getKey())
            ).forEach(entry -> springHeaders.put(entry.getKey(), entry.getValue()));
        }
    }


    private boolean customize(String key) {
        if (MessageConst.STRING_HASH_SET.contains(key)) {
            return false;
        }
        return !key.startsWith(RocketHeaders.PREFIX) || !MessageConst.STRING_HASH_SET.contains(RocketHeaders.replacePrefix(key));
    }
}
