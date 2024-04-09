package org.springframework.rocket.test.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder {

    private final Map<String, Object> map;

    private MapBuilder() {
        this.map = new HashMap<>(64);
    }

    public MapBuilder put(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

    public MapBuilder putAll(Map<String, Object> aMap) {
        this.map.putAll(aMap);
        return this;
    }

    public Map<String, Object> build() {
        return this.map;
    }

    public static MapBuilder builder() {
        return new MapBuilder();
    }
}
