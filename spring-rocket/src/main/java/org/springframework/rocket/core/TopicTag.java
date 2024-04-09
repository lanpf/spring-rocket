package org.springframework.rocket.core;

import org.springframework.util.StringUtils;

public record TopicTag(String topic, String tag) {

    public static TopicTag of(String destination) {
        if (destination == null) {
            return null;
        }
        String[] destinationArrays = destination.split(":", 2);
        return new TopicTag(destinationArrays[0], destinationArrays.length > 1 ? destinationArrays[1] : null);
    }

    @Override
    public String toString() {
        return StringUtils.hasText(tag) ? topic + ":" + tag : topic;
    }
}
