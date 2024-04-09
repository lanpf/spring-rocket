package org.springframework.rocket.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

@RequiredArgsConstructor
@Getter
public class DefaultRocketProducerFactory implements RocketProducerFactory {

    private final Map<String, Object> defaultProperties;

    public DefaultRocketProducerFactory(Properties defaultProperties) {
        this(PropertiesUtils.asMap(defaultProperties));
    }

    @Override
    public MQProducer create(String groupId, Map<String, Object> overrideProperties) {
        String group = getGroupId(groupId, overrideProperties);
        ProducerProperties producerProperties = new ProducerProperties(PropertiesUtils.asMap(getDefaultProperties(), overrideProperties));

        boolean aclEnabled = StringUtils.hasText(producerProperties.getAccessKey()) && StringUtils.hasText(producerProperties.getSecretKey());
        RPCHook rpcHook = aclEnabled ? new AclClientRPCHook(new SessionCredentials(producerProperties.getAccessKey(), producerProperties.getSecretKey())) : null;
        DefaultMQProducer producer = new DefaultMQProducer(
                group,
                rpcHook,
                Boolean.TRUE.equals(producerProperties.getTraceEnabled()),
                producerProperties.getCustomizedTraceTopic()
        );

        if (aclEnabled) {
            producer.setVipChannelEnabled(false);
        }


        JavaUtils.INSTANCE
                .acceptIfHasText(producerProperties.getNamespace(), producer::setNamespace)
                .acceptIfHasText(producerProperties.getInstanceName(), producer::setInstanceName)
                .acceptIfHasText(producerProperties.getNameServer(), producer::setNamesrvAddr)
                .acceptIfNotNull(producerProperties.getTlsEnabled(), producer::setUseTLS)
                .acceptIfNotNull(producerProperties.getAccessChannel(), producer::setAccessChannel)


                .acceptIfNotNull(producerProperties.getSendTimeoutMillis(), producer::setSendMsgTimeout)
                .acceptIfNotNull(producerProperties.getSyncRetries(), producer::setRetryTimesWhenSendFailed)
                .acceptIfNotNull(producerProperties.getAsyncRetries(), producer::setRetryTimesWhenSendAsyncFailed)
                .acceptIfNotNull(producerProperties.getRetryAnotherBroker(), producer::setRetryAnotherBrokerWhenNotStoreOK)
                .acceptIfNotNull(producerProperties.getMaxSize(), producer::setMaxMessageSize)
                .acceptIfNotNull(producerProperties.getCompressThreshold(), producer::setCompressMsgBodyOverHowmuch);

        return producer;
    }
}
