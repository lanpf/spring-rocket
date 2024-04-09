package org.springframework.rocket.client;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.AccessChannel;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Setter
@Getter
public class ClientProperties {

    public static final String NAMESPACE = "namespace";
    public static final String GROUP_ID = "groupId";
    public static final String ACCESS_KEY = "accessKey";
    public static final String SECRET_KEY = "secretKey";
    public static final String TRACE_ENABLED = "traceEnabled";
    public static final String CUSTOMIZED_TRACE_TOPIC = "customizedTraceTopic";
    public static final String INSTANCE_NAME = "instanceName";
    public static final String NAME_SERVER = "nameServer";
    public static final String TLS_ENABLED = "tlsEnabled";
    public static final String ACCESS_CHANNEL = "accessChannel";
    private String namespace;
    private String groupId;
    private String accessKey;
    private String secretKey;
    private Boolean traceEnabled;
    private String customizedTraceTopic;
    private String instanceName;
    private String nameServer;
    private Boolean tlsEnabled;
    private AccessChannel accessChannel;

    public ClientProperties(Map<String, Object> properties) {
        if (ObjectUtils.isEmpty(properties)) {
            return;
        }
        this.namespace = PropertiesUtils.extractAsString(properties, NAMESPACE);
        this.groupId = PropertiesUtils.extractAsString(properties, GROUP_ID);
        this.accessKey = PropertiesUtils.extractAsString(properties, ACCESS_KEY);
        this.secretKey = PropertiesUtils.extractAsString(properties, SECRET_KEY);
        this.traceEnabled = PropertiesUtils.extractAsBoolean(properties, TRACE_ENABLED);
        this.customizedTraceTopic = PropertiesUtils.extractAsString(properties, CUSTOMIZED_TRACE_TOPIC);
        this.instanceName = PropertiesUtils.extractAsString(properties, INSTANCE_NAME);
        this.nameServer = PropertiesUtils.extractAsString(properties, NAME_SERVER);
        this.tlsEnabled = PropertiesUtils.extractAsBoolean(properties, TLS_ENABLED);
        this.accessChannel = PropertiesUtils.extractAsEnum(properties, ACCESS_CHANNEL, AccessChannel.class);
    }
}
