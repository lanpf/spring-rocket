package org.springframework.rocket.support;

import org.apache.rocketmq.common.message.Message;
import org.springframework.rocket.core.Delay;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class RocketHeaderUtils {
    public static final BiConsumer<Map<String, Object>, String> TAGS_HEADER_SET = (headers, headerValue) -> {
        if (StringUtils.hasText(headerValue)) {
            headers.put(RocketHeaders.TAGS, headerValue);
        }
    };
    public static final BiConsumer<Message, Map<String, Object>> TAGS_HEADER_GET = (rocketMessage, springHeaders) -> {
        String headerValue = PropertiesUtils.extractAsString(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.TAGS);
        if (StringUtils.hasText(headerValue)) {
            rocketMessage.setTags(headerValue);
        }
    };

    public static final BiConsumer<Map<String, Object>, String> KEYS_HEADER_SET = (headers, headerValue) -> {
        if (StringUtils.hasText(headerValue)) {
            headers.put(RocketHeaders.KEYS, headerValue);
        }
    };
    public static final BiConsumer<Message, Map<String, Object>> KEYS_HEADER_GET = (rocketMessage, springHeaders) -> {
        String headerValue = PropertiesUtils.extractAsString(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.KEYS);
        if (StringUtils.hasText(headerValue)) {
            rocketMessage.setKeys(headerValue);
        }
    };


    public static final BiConsumer<Map<String, Object>, Integer> FLAG_HEADER_SET = (headers, headerValue) -> {
        if (headerValue != null) {
            headers.put(RocketHeaders.FLAG, headerValue);
        }
    };
    public static final BiConsumer<Message, Map<String, Object>> FLAG_HEADER_GET = (rocketMessage, springHeaders) -> {
        Integer headerValue = Optional.ofNullable(PropertiesUtils.extractAsInteger(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.FLAG))
                .orElse(0);
        rocketMessage.setFlag(headerValue);
    };


    public static final BiConsumer<Map<String, Object>, Boolean> WAIT_STORE_MSG_OK_HEADER_SET = (headers, headerValue) -> {
        if (headerValue != null) {
            headers.put(RocketHeaders.WAIT_STORE_MSG_OK, headerValue);
        }
    };
    public static final BiConsumer<Message, Map<String, Object>> WAIT_STORE_MSG_OK_HEADER_GET = (rocketMessage, springHeaders) -> {
        Boolean headerValue = Optional.ofNullable(PropertiesUtils.extractAsBoolean(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.WAIT_STORE_MSG_OK))
                .orElse(Boolean.TRUE);
        rocketMessage.setWaitStoreMsgOK(headerValue);
    };

    public static final BiConsumer<Map<String, Object>, Delay> DELAY_HEADER_SET = (headers, delay) -> {
        if (delay == null || delay.mode() == null || delay.value() == null) {
            return;
        }
        switch (delay.mode()) {
            case DELAY_LEVEL -> headers.put(RocketHeaders.DELAY_TIME_LEVEL, delay.value());
            case DELAY_SECONDS -> headers.put(RocketHeaders.TIMER_DELAY_SEC, delay.value());
            case DELAY_MILLIS -> headers.put(RocketHeaders.TIMER_DELAY_MS, delay.value());
            case DELIVER_TIME_MILLIS -> headers.put(RocketHeaders.TIMER_DELIVER_MS, delay.value());
            default -> throw new UnsupportedOperationException();
        }
    };
    public static final BiConsumer<Message, Map<String, Object>> DELAY_HEADER_GET = (rocketMessage, springHeaders) -> {
        Long timerDeliverMs = PropertiesUtils.extractAsLong(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.TIMER_DELIVER_MS);
        Long timerDelayMs = null;
        Long timerDelaySec = null;
        Integer delayTimeLevel = null;
        if (timerDeliverMs == null) {
            timerDelayMs = PropertiesUtils.extractAsLong(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.TIMER_DELAY_MS);
            if (timerDelayMs == null) {
                timerDelaySec = PropertiesUtils.extractAsLong(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.TIMER_DELAY_SEC);
                if (timerDelaySec == null) {
                    delayTimeLevel = PropertiesUtils.extractAsInteger(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.DELAY_TIME_LEVEL);
                }
            }
        }
        JavaUtils.INSTANCE
                .acceptIfNotNull(timerDeliverMs, rocketMessage:: setDeliverTimeMs)
                .acceptIfNotNull(timerDelayMs, rocketMessage::setDelayTimeMs)
                .acceptIfNotNull(timerDelaySec, rocketMessage::setDelayTimeSec)
                .acceptIfNotNull(delayTimeLevel, rocketMessage::setDelayTimeLevel);
    };

    public static final BiConsumer<Map<String, Object>, String> SHARDING_KEY_HEADER_SET = (headers, headerValue) -> {
        if (StringUtils.hasText(headerValue)) {
            headers.put(RocketHeaders.SHARDING_KEY, headerValue);
        }
    };

    public static final BiConsumer<Message, Map<String, Object>> TRANSACTION_ID_HEADER_GET = (rocketMessage, springHeaders) -> {
        String headerValue = PropertiesUtils.extractAsString(header -> RocketHeaders.find(springHeaders, header), RocketHeaders.TRANSACTION_ID);
        if (StringUtils.hasText(headerValue)) {
            rocketMessage.setTransactionId(headerValue);
        }
    };

    public static final BiConsumer<Map<String, Object>, Object> TRANSACTION_ARG_HEADER_SET = (headers, headerValue) -> {
        if (!ObjectUtils.isEmpty(headerValue)) {
            headers.put(RocketHeaders.TRANSACTION_ARG, headerValue);
        }
    };
}
