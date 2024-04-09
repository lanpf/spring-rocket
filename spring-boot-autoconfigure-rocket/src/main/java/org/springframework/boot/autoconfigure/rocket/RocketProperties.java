package org.springframework.boot.autoconfigure.rocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.rocket.client.ClientProperties;
import org.springframework.rocket.client.ProducerProperties;
import org.springframework.rocket.client.PushConsumerProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Data
@ConfigurationProperties(prefix = RocketProperties.PREFIX)
public class RocketProperties {
    public static final String PREFIX = "rocket";

    private String nameServer;

    /**
     * Additional properties, common to producers and consumers, used to configure the
     * client. {@link ClientProperties}
     */
    private final Map<String, String> properties = new HashMap<>();

    /**
     * producer properties
     */
    private final Producer producer = new Producer();

    /**
     * consumer properties
     */
    private final Consumer consumer = new Consumer();

    /**
     * common listener properties
     */
    private final Listener listener = new Listener();

    /**
     * common template properties
     */
    private final Template template = new Template();

    public Properties buildProducerProperties() {
        Properties result = this.getProducer().buildProperties();
        this.getProperties().forEach(result::putIfAbsent);
        result.putIfAbsent(ClientProperties.NAME_SERVER, nameServer);
        return result;
    }

    public Properties buildPushConsumerProperties() {
        Properties result = this.getConsumer().getPush().buildProperties();
        this.getProperties().forEach(result::putIfAbsent);
        result.putIfAbsent(ClientProperties.NAME_SERVER, nameServer);
        return result;
    }

    @Data
    public static class Producer {
        private String groupId;
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration sendTimeout;
        private Integer syncRetries;
        private Integer asyncRetries;
        private Boolean retryAnotherBroker;
        private Integer maxSize;
        private Integer compressThreshold;

        public Properties buildProperties() {
            Properties properties = new Properties();
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            map.from(this::getGroupId).to(value -> properties.put(ProducerProperties.GROUP_ID, value));
            map.from(this::getSendTimeout).as(Duration::toMillis).to(value -> properties.put(ProducerProperties.SEND_TIMEOUT_MILLIS, value));
            map.from(this::getSyncRetries).to(value -> properties.put(ProducerProperties.SYNC_RETRIES, value));
            map.from(this::getAsyncRetries).to(value -> properties.put(ProducerProperties.ASYNC_RETRIES, value));
            map.from(this::getRetryAnotherBroker).to(value -> properties.put(ProducerProperties.RETRY_ANOTHER_BROKER, value));
            map.from(this::getMaxSize).to(value -> properties.put(ProducerProperties.MAX_SIZE, value));
            map.from(this::getCompressThreshold).to(value -> properties.put(ProducerProperties.COMPRESS_THRESHOLD, value));
            return properties;
        }
    }

    @Data
    public static class Consumer {
        private final Push push = new Push();

        @Data
        public static class Push {
            private String groupId;
            private String messageModel;
            private Integer pullBatchSize;
            private String allocateMessageQueueStrategy;
            private Integer minConsumeThreads;
            private Integer maxConsumeThreads;
            private Integer consumeBatchSize;
            @DurationUnit(ChronoUnit.MINUTES)
            private Duration consumeTimeout;
            @DurationUnit(ChronoUnit.MILLIS)
            private Duration shutdownAwaitTermination;
            private Integer retries;
            @DurationUnit(ChronoUnit.MILLIS)
            private Duration suspendCurrentQueueTime = Duration.ofMillis(1000);
            private Integer retryDelayLevel;

            public Properties buildProperties() {
                Properties properties = new Properties();
                PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
                map.from(this::getGroupId).to(value -> properties.put(PushConsumerProperties.GROUP_ID, value));
                map.from(this::getMessageModel).to(value -> properties.put(PushConsumerProperties.MESSAGE_MODEL, value));
                map.from(this::getPullBatchSize).to(value -> properties.put(PushConsumerProperties.PULL_BATCH_SIZE, value));
                map.from(this::getAllocateMessageQueueStrategy).to(value -> properties.put(PushConsumerProperties.ALLOCATE_MESSAGE_QUEUE_STRATEGY, value));
                map.from(this::getMinConsumeThreads).to(value -> properties.put(PushConsumerProperties.MIN_CONSUME_THREADS, value));
                map.from(this::getMaxConsumeThreads).to(value -> properties.put(PushConsumerProperties.MAX_CONSUME_THREADS, value));
                map.from(this::getConsumeTimeout).as(Duration::toMinutes).to(value -> properties.put(PushConsumerProperties.CONSUME_TIMEOUT_MINUTES, value));
                map.from(this::getShutdownAwaitTermination).as(Duration::toMillis).to(value -> properties.put(PushConsumerProperties.SHUTDOWN_AWAIT_TERMINATION_MILLIS, value));
                map.from(this::getRetries).to(value -> properties.put(PushConsumerProperties.RETRIES, value));
                map.from(this::getSuspendCurrentQueueTime).as(Duration::toMillis).to(value -> properties.put(PushConsumerProperties.SUSPEND_CURRENT_QUEUE_TIME_MILLIS, value));
                map.from(this::getRetryDelayLevel).to(value -> properties.put(PushConsumerProperties.RETRY_DELAY_LEVEL, value));
                return properties;
            }
        }
    }

    @Data
    public static class Listener {
        /**
         * Whether listener is concurrency
         */
        private Boolean concurrency;
    }

    @Data
    public static class Template {
        /**
         * Default destination to which messages are sent.
         */
        private String defaultDestination;
    }

}
