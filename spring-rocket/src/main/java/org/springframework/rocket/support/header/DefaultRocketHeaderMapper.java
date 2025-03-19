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
    public void fromHeaders(MessageHeaders headers, Message rocketMessage) {
        Objects.requireNonNull(rocketMessage, "rocketMessage must not be null");
        if (ObjectUtils.isEmpty(headers)) {
            return;
        }
        RocketHeaderUtils.TAGS_HEADER_GET.accept(rocketMessage, headers);
        RocketHeaderUtils.KEYS_HEADER_GET.accept(rocketMessage, headers);
        RocketHeaderUtils.FLAG_HEADER_GET.accept(rocketMessage, headers);
        RocketHeaderUtils.WAIT_STORE_MSG_OK_HEADER_GET.accept(rocketMessage, headers);
        RocketHeaderUtils.DELAY_HEADER_GET.accept(rocketMessage, headers);
        RocketHeaderUtils.TRANSACTION_ID_HEADER_GET.accept(rocketMessage, headers);

        RocketHeaderUtils.REPLY_HEADER_GET.accept(rocketMessage, headers);

        headers.entrySet().stream()
                .filter(entry -> customize(entry.getKey()))
                .forEach(entry -> MessageAccessor.putProperty(rocketMessage, entry.getKey(), String.valueOf(entry.getValue())));
    }

    @Override
    public void toHeaders(Message rocketMessage, Map<String, Object> headers) {
        Objects.requireNonNull(rocketMessage, "rocketMessage must not be null");
        headers.put(RocketHeaders.RECEIVED_TOPIC, rocketMessage.getTopic());
        JavaUtils.INSTANCE
                .acceptIfHasText(rocketMessage.getTags(), value -> headers.put(RocketHeaders.RECEIVED_TAGS, value))
                .acceptIfHasText(rocketMessage.getKeys(), value -> headers.put(RocketHeaders.RECEIVED_KEYS, value))
                .acceptIfNotNull(rocketMessage.getFlag(), value -> headers.put(RocketHeaders.RECEIVED_FLAG, value))
                .acceptIfNotNull(rocketMessage.isWaitStoreMsgOK(), value -> headers.put(RocketHeaders.RECEIVED_WAIT_STORE_MSG_OK, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELIVER_MS), value -> headers.put(RocketHeaders.RECEIVED_DELIVER_TIME_MILLIS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELAY_MS), value -> headers.put(RocketHeaders.RECEIVED_DELAY_MILLIS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_TIMER_DELAY_SEC), value -> headers.put(RocketHeaders.RECEIVED_DELAY_SECONDS, value))
                .acceptIfHasText(rocketMessage.getProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL), value -> headers.put(RocketHeaders.RECEIVED_DELAY_LEVEL, value))
                .acceptIfHasText(rocketMessage.getTransactionId(), value -> headers.put(RocketHeaders.RECEIVED_TRANSACTION_ID, value));

        if (rocketMessage instanceof MessageExt messageExt) {
            JavaUtils.INSTANCE
                    .acceptIfHasText(messageExt.getMsgId(), value -> headers.put(RocketHeaders.RECEIVED_MESSAGE_ID, value))
                    .acceptIfNotNull(messageExt.getQueueId(), value -> headers.put(RocketHeaders.RECEIVED_QUEUE_ID, value))
                    .acceptIfNotNull(messageExt.getQueueOffset(), value -> headers.put(RocketHeaders.RECEIVED_QUEUE_OFFSET, value))
                    .acceptIfNotNull(messageExt.getBornTimestamp(), value -> headers.put(RocketHeaders.RECEIVED_BORN_TIMESTAMP, value))
                    .acceptIfHasText(messageExt.getBornHostString(), value -> headers.put(RocketHeaders.RECEIVED_BORN_HOST, value))
                    .acceptIfNotNull(messageExt.getSysFlag(), value -> headers.put(RocketHeaders.RECEIVED_SYS_FLAG, value));
        }
        if (!ObjectUtils.isEmpty(rocketMessage.getProperties())) {
            rocketMessage.getProperties().entrySet().stream()
                    .filter(entry -> !MessageHeaders.ID.equals(entry.getKey())
                            && !MessageHeaders.TIMESTAMP.equals(entry.getKey())
                            && customize(entry.getKey())
            ).forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        }
    }

    private boolean customize(String key) {
        if (MessageConst.STRING_HASH_SET.contains(key)) {
            return false;
        }
        return !key.startsWith(RocketHeaders.PREFIX) || !MessageConst.STRING_HASH_SET.contains(RocketHeaders.replacePrefix(key));
    }
}