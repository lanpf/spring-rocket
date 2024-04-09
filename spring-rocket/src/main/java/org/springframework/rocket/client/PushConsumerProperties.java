package org.springframework.rocket.client;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragelyByCircle;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueByConfig;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueByMachineRoom;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueConsistentHash;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
@Setter
public class PushConsumerProperties extends ConsumerProperties {

    public static final String PULL_BATCH_SIZE = "pullBatchSize";
    public static final String ALLOCATE_MESSAGE_QUEUE_STRATEGY = "allocateMessageQueueStrategy";
    public static final String MIN_CONSUME_THREADS = "minConsumeThreads";
    public static final String MAX_CONSUME_THREADS = "maxConsumeThreads";
    public static final String CONSUME_TIMEOUT_MINUTES = "consumeTimeoutMinutes";
    public static final String CONSUME_BATCH_SIZE = "consumeBatchSize";
    public static final String SHUTDOWN_AWAIT_TERMINATION_MILLIS = "shutdownAwaitTerminationMillis";
    public static final String RETRIES = "retries";

    public static final String SUSPEND_CURRENT_QUEUE_TIME_MILLIS = "suspendCurrentQueueTimeMillis";
    public static final String RETRY_DELAY_LEVEL = "retryDelayLevel";

    private Integer pullBatchSize;
    private AllocateMessageQueueStrategy allocateMessageQueueStrategy;
    private Integer minConsumeThreads;
    private Integer maxConsumeThreads;
    private Long consumeTimeoutMinutes;
    private Integer consumeBatchSize;
    private Long shutdownAwaitTerminationMillis;
    private Integer retries;


    private Long suspendCurrentQueueTimeMillis;
    private Integer retryDelayLevel;


    public PushConsumerProperties(Map<String, Object> properties) {
        super(properties);
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy(PropertiesUtils.extractAsString(properties, ALLOCATE_MESSAGE_QUEUE_STRATEGY));
        if (ObjectUtils.isEmpty(properties)) {
            return;
        }
        this.pullBatchSize = PropertiesUtils.extractAsInteger(properties, PULL_BATCH_SIZE);
        this.minConsumeThreads = PropertiesUtils.extractAsInteger(properties, MIN_CONSUME_THREADS);
        this.maxConsumeThreads = PropertiesUtils.extractAsInteger(properties, MAX_CONSUME_THREADS);
        this.consumeTimeoutMinutes = PropertiesUtils.extractAsLong(properties, CONSUME_TIMEOUT_MINUTES);
        this.consumeBatchSize = PropertiesUtils.extractAsInteger(properties, CONSUME_BATCH_SIZE);
        this.shutdownAwaitTerminationMillis = PropertiesUtils.extractAsLong(properties, SHUTDOWN_AWAIT_TERMINATION_MILLIS);
        this.retries = PropertiesUtils.extractAsInteger(properties, RETRIES);

        this.suspendCurrentQueueTimeMillis = PropertiesUtils.extractAsLong(properties, SUSPEND_CURRENT_QUEUE_TIME_MILLIS);
        this.retryDelayLevel = PropertiesUtils.extractAsInteger(properties, RETRY_DELAY_LEVEL);
    }


    private AllocateMessageQueueStrategy allocateMessageQueueStrategy(String strategy) {
        if (!StringUtils.hasText(strategy)) {
            return new AllocateMessageQueueAveragely();
        }
        return switch (strategy) {
            case "AVG_BY_CIRCLE" -> new AllocateMessageQueueAveragelyByCircle();
            case "CONFIG" -> new AllocateMessageQueueByConfig();
            case "MACHINE_ROOM" -> new AllocateMessageQueueByMachineRoom();
            case "CONSISTENT_HASH" -> new AllocateMessageQueueConsistentHash();
            default -> new AllocateMessageQueueAveragely();
        };
    }
}
