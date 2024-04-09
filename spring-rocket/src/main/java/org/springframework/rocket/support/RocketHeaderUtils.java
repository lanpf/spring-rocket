package org.springframework.rocket.support;

import org.apache.rocketmq.common.message.Message;
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
}
