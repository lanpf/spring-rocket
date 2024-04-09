package org.springframework.rocket.client;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Getter
@Setter
public class ConsumerProperties extends ClientProperties {

    public static final String MESSAGE_MODEL = "messageModel";
    private MessageModel messageModel;

    public ConsumerProperties(Map<String, Object> properties) {
        super(properties);
        if (ObjectUtils.isEmpty(properties)) {
            return;
        }
        this.messageModel = PropertiesUtils.extractAsEnum(properties, MESSAGE_MODEL, MessageModel.class);
    }
}
