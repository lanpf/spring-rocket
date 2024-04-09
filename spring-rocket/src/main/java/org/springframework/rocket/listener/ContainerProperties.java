package org.springframework.rocket.listener;

import lombok.Getter;
import lombok.Setter;
import org.springframework.rocket.core.FilterExpressionType;

import java.util.Properties;

@Setter
@Getter
public class ContainerProperties {

    private String groupId;
    private String topic;
    private String filterExpressionType = FilterExpressionType.TAG.name();
    private String filterExpression = "*";

    private Boolean batchListener;

    private Properties rocketConsumerProperties = new Properties();

    private Object messageListener;

    public void updateContainerProperties() {

    }
}
