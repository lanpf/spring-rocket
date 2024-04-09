package org.springframework.rocket.client;

import java.util.Map;

public interface RocketClientFactory<T> {

    default T create() {
        return create(null);
    }

    T create(Map<String, Object> overrideProperties);
}
