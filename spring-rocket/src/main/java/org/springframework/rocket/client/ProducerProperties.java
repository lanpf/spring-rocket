package org.springframework.rocket.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Getter
@Setter
public class ProducerProperties extends ClientProperties {
    public static final String SEND_TIMEOUT_MILLIS = "sendTimeoutMillis";
    public static final String SYNC_RETRIES = "syncRetries";
    public static final String ASYNC_RETRIES = "asyncRetries";
    public static final String RETRY_ANOTHER_BROKER = "retryAnotherBroker";
    public static final String MAX_SIZE = "maxSize";
    public static final String COMPRESS_THRESHOLD = "compressThreshold";
    public static final String TRANSACTIONAL = "transactional";

    private Integer sendTimeoutMillis;
    private Integer syncRetries;
    private Integer asyncRetries;
    private Boolean retryAnotherBroker;
    private Integer maxSize;
    private Integer compressThreshold;
    private Boolean transactional;


    public ProducerProperties(Map<String, Object> properties) {
        super(properties);
        if (ObjectUtils.isEmpty(properties)) {
            return;
        }
        this.sendTimeoutMillis = PropertiesUtils.extractAsInteger(properties, SEND_TIMEOUT_MILLIS);
        this.syncRetries = PropertiesUtils.extractAsInteger(properties, SYNC_RETRIES);
        this.asyncRetries = PropertiesUtils.extractAsInteger(properties, ASYNC_RETRIES);
        this.retryAnotherBroker = PropertiesUtils.extractAsBoolean(properties, RETRY_ANOTHER_BROKER);
        this.maxSize = PropertiesUtils.extractAsInteger(properties, MAX_SIZE);
        this.compressThreshold = PropertiesUtils.extractAsInteger(properties, COMPRESS_THRESHOLD);
        this.transactional = PropertiesUtils.extractAsBoolean(properties, TRANSACTIONAL);
    }
}
