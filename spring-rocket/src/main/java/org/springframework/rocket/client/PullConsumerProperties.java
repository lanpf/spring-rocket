package org.springframework.rocket.client;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Getter
@Setter
public class PullConsumerProperties extends ClientProperties {
    public static final String MESSAGE_MODEL = "messageModel";
    public static final String PULL_BATCH_SIZE = "pullBatchSize";
    public static final String PULL_THREADS = "pullThreads";
    public static final String POLL_TIMEOUT_MILLIS = "pollTimeoutMillis";
    private MessageModel messageModel;
    private Integer pullBatchSize;
    private Integer pullThreads;
    private Long pollTimeoutMillis;

    public PullConsumerProperties(Map<String, Object> properties) {
        super(properties);
        if (ObjectUtils.isEmpty(properties)) {
            return;
        }
        this.messageModel = PropertiesUtils.extractAsEnum(properties, MESSAGE_MODEL, MessageModel.class);
        this.pullBatchSize = PropertiesUtils.extractAsInteger(properties, PULL_BATCH_SIZE);
        this.pullThreads = PropertiesUtils.extractAsInteger(properties, PULL_THREADS);
        this.pollTimeoutMillis = PropertiesUtils.extractAsLong(properties, POLL_TIMEOUT_MILLIS);
    }
}
