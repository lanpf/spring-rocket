package org.springframework.rocket.support;

import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @see MessageConst#STRING_HASH_SET
 */
public abstract class RocketHeaders {

    public static final String PREFIX = "rocket_";
    /**
     * --------------------    common header    --------------------
     */
    public static final String TOPIC = PREFIX + "TOPIC";
    public static final String TAGS = PREFIX + MessageConst.PROPERTY_TAGS;
    public static final String KEYS = PREFIX + MessageConst.PROPERTY_KEYS;
    public static final String FLAG = PREFIX + "FLAG";
    public static final String WAIT_STORE_MSG_OK = PREFIX + MessageConst.PROPERTY_WAIT_STORE_MSG_OK;


    public static final String RECEIVED = PREFIX + "received_";
    /**
     * --------------------    received message header    --------------------
     */
    public static final String RECEIVED_MESSAGE_ID = RECEIVED + "MESSAGE_ID";
    public static final String RECEIVED_QUEUE_ID = RECEIVED + "QUEUE_ID";
    public static final String RECEIVED_QUEUE_OFFSET = RECEIVED + "QUEUE_OFFSET";
    public static final String RECEIVED_BORN_HOST = RECEIVED + "BORN_HOST";
    public static final String RECEIVED_BORN_TIMESTAMP = RECEIVED + "BORN_TIMESTAMP";
    public static final String RECEIVED_SYS_FLAG = RECEIVED + "SYS_FLAG";
    /**
     * --------------------    received common header    --------------------
     */
    public static final String RECEIVED_TOPIC = RECEIVED + "TOPIC";
    public static final String RECEIVED_TAGS = RECEIVED + MessageConst.PROPERTY_TAGS;
    public static final String RECEIVED_KEYS = RECEIVED + MessageConst.PROPERTY_KEYS;
    public static final String RECEIVED_FLAG = RECEIVED + "FLAG";
    public static final String RECEIVED_WAIT_STORE_MSG_OK = RECEIVED + MessageConst.PROPERTY_WAIT_STORE_MSG_OK;

    public static String replacePrefix(String key) {
        return StringUtils.hasText(key) ? key.replaceFirst("^" + PREFIX, "") : null;
    }


    public static Object find(Map<String, Object> headers, String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        return findFirst(headers, header, replacePrefix(header));
    }

    public static Object findFirst(Map<String, Object> headers, String... header) {
        if (ObjectUtils.isEmpty(headers) || ObjectUtils.isEmpty(header)) {
            return null;
        }
        for (String h : header) {
            Object value = headers.get(h);
            if (!ObjectUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }
}
