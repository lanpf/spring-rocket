package org.springframework.rocket.client;

import org.apache.rocketmq.client.producer.MQProducer;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

public interface RocketProducerFactory extends RocketClientFactory<MQProducer> {

    Map<String, Object> getDefaultProperties();

    default String getGroupId(String groupId, Map<String, Object> overrideProperties) {
        String overrideGroupId = PropertiesUtils.extractAsString(overrideProperties, ClientProperties.GROUP_ID);
        if (StringUtils.hasText(overrideGroupId)) {
            return overrideGroupId;
        }
        if (StringUtils.hasText(groupId)) {
            return groupId;
        }
        return PropertiesUtils.extractAsString(getDefaultProperties(), ClientProperties.GROUP_ID);
    }

    @Override
    default MQProducer create(Map<String, Object> overrideProperties) {
        return create(null, overrideProperties);
    }

    MQProducer create(String groupId, Map<String, Object> overrideProperties);
}
