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
    /**
     * --------------------    delay header    --------------------
     */
    public static final String DELAY_TIME_LEVEL = PREFIX + MessageConst.PROPERTY_DELAY_TIME_LEVEL;
    public static final String TIMER_DELIVER_MS = PREFIX + MessageConst.PROPERTY_TIMER_DELIVER_MS;
    public static final String TIMER_DELAY_SEC = PREFIX + MessageConst.PROPERTY_TIMER_DELAY_SEC;
    public static final String TIMER_DELAY_MS = PREFIX + MessageConst.PROPERTY_TIMER_DELAY_MS;
    /**
     * --------------------    sharding header   --------------------
     */
     public static final String SHARDING_KEY = PREFIX + "SHARDING_KEY";
    /**
     * --------------------    transaction header   --------------------
     */
    public static final String TRANSACTION_ARG = PREFIX + "TRANSACTION_ARG";
    public static final String TRANSACTION_ID = PREFIX + "TRANSACTION_ID";
    /**
     * --------------------    reply header    --------------------
     */
    public static final String REPLY_TOPIC = PREFIX + "REPLY_TOPIC";
    public static final String MESSAGE_TYPE = PREFIX + MessageConst.PROPERTY_MESSAGE_TYPE;
    public static final String CORRELATION_ID = PREFIX + MessageConst.PROPERTY_CORRELATION_ID;
    public static final String MESSAGE_REPLY_TO_CLIENT = PREFIX + MessageConst.PROPERTY_MESSAGE_REPLY_TO_CLIENT;
    public static final String MESSAGE_TTL = PREFIX + MessageConst.PROPERTY_MESSAGE_TTL;


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
    /**
     * --------------------    received delay header    --------------------
     */
    public static final String RECEIVED_DELIVER_TIME_MILLIS = RECEIVED + MessageConst.PROPERTY_TIMER_DELIVER_MS;
    public static final String RECEIVED_DELAY_MILLIS = RECEIVED + MessageConst.PROPERTY_TIMER_DELAY_MS;
    public static final String RECEIVED_DELAY_SECONDS = RECEIVED + MessageConst.PROPERTY_TIMER_DELAY_SEC;
    public static final String RECEIVED_DELAY_LEVEL = RECEIVED + MessageConst.PROPERTY_DELAY_TIME_LEVEL;
    /**
     * --------------------    received transaction header    --------------------
     */
    public static final String RECEIVED_TRANSACTION_ID = RECEIVED + "TRANSACTION_ID";


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