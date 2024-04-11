package org.springframework.boot.autoconfigure.rocket;

import org.springframework.rocket.core.RocketTemplate;

@FunctionalInterface
public interface RocketTemplateCustomizer {

    void customize(RocketTemplate rocketTemplate);
}
