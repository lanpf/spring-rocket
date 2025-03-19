package org.springframework.rocket.support.header;

import org.apache.rocketmq.common.message.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Map;

public interface RocketHeaderMapper {

    /**
     * Map from the given Spring Messaging headers to Rocket message headers.
     * <p>
     * Commonly used in the outbound flow when a Spring message is being converted to a
     * Pulsar message in order to be written out to Pulsar topic (outbound).
     * @param headers the Spring messaging headers
     * @param rocketMessage the Rocket message
     */
    void fromHeaders(MessageHeaders headers, Message rocketMessage);

    /**
     * Map the headers from the given Rocket message to Spring Messaging headers.
     * <p>
     * Commonly used in the inbound flow when an incoming Pulsar message is being
     * converted to a Spring message.
     * @param rocketMessage the Rocket message containing the headers to map
     * @param headers the map of Spring messaging headers
     */
    void toHeaders(Message rocketMessage, Map<String, Object> headers);

}
