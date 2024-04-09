package org.springframework.rocket.client;

import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

public interface RocketConsumerFactory<T> extends RocketClientFactory<T> {

//    boolean isAutoCommit();

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
    default T create(Map<String, Object> overrideProperties) {
        return create(null, overrideProperties);
    }

    T create(String groupId, Map<String, Object> overrideProperties);

}
