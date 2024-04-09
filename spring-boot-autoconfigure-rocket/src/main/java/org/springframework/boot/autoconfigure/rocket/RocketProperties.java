package org.springframework.boot.autoconfigure.rocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.rocket.client.ClientProperties;
import org.springframework.rocket.client.ProducerProperties;

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
     * common template properties
     */
    private final Template template = new Template();

    public Properties buildProducerProperties() {
        Properties result = this.getProducer().buildProperties();
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
    public static class Template {
        /**
         * Default destination to which messages are sent.
         */
        private String defaultDestination;

    }

}
