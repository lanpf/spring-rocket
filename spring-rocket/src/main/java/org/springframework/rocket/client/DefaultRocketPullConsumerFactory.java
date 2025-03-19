package org.springframework.rocket.client;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.LitePullConsumer;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

@RequiredArgsConstructor
public class DefaultRocketPullConsumerFactory implements RocketPullConsumerFactory {

    private final Map<String, Object> properties;

    public DefaultRocketPullConsumerFactory(Properties properties) {
        this(PropertiesUtils.asMap(properties));
    }

    @Override
    public LitePullConsumer create(Map<String, Object> overrideProperties) {
        PullConsumerProperties consumerProperties = new PullConsumerProperties(PropertiesUtils.asMap(properties, overrideProperties));
        String group = PropertiesUtils.extractAsString(overrideProperties, ClientProperties.GROUP_ID);

        boolean aclEnabled = StringUtils.hasText(consumerProperties.getAccessKey()) && StringUtils.hasText(consumerProperties.getSecretKey());
        RPCHook rpcHook = aclEnabled ? new AclClientRPCHook(new SessionCredentials(consumerProperties.getAccessKey(), consumerProperties.getSecretKey())) : null;
        DefaultLitePullConsumer consumer = new DefaultLitePullConsumer(group, rpcHook);

        if (aclEnabled) {
            consumer.setVipChannelEnabled(false);
        }

        JavaUtils.INSTANCE
                .acceptIfNotNull(consumerProperties.getTraceEnabled(), consumer::setEnableMsgTrace)
                .acceptIfHasText(consumerProperties.getCustomizedTraceTopic(), consumer::setCustomizedTraceTopic)

//                .acceptIfHasText(consumerProperties.getNamespace(), consumer::setNamespace)
                .acceptIfHasText(consumerProperties.getNamespace(), consumer::setNamespaceV2)
                .acceptIfHasText(consumerProperties.getInstanceName(), consumer::setInstanceName)
                .acceptIfHasText(consumerProperties.getNameServer(), consumer::setNamesrvAddr)
                .acceptIfNotNull(consumerProperties.getTlsEnabled(), consumer::setUseTLS)
                .acceptIfNotNull(consumerProperties.getAccessChannel(), consumer::setAccessChannel)

                .acceptIfNotNull(consumerProperties.getMessageModel(), consumer::setMessageModel)

                .acceptIfNotNull(consumerProperties.getPullBatchSize(), consumer::setPullBatchSize)
                .acceptIfNotNull(consumerProperties.getPullThreads(), consumer::setPullThreadNums)
                .acceptIfNotNull(consumerProperties.getPollTimeoutMillis(), consumer::setPollTimeoutMillis);

        return consumer;
    }

}
