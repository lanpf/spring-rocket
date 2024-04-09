package org.springframework.rocket.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.rocket.support.JavaUtils;
import org.springframework.rocket.support.PropertiesUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

@RequiredArgsConstructor
@Getter
public class DefaultRocketPushConsumerFactory implements RocketPushConsumerFactory {

    private final Map<String, Object> defaultProperties;

    public DefaultRocketPushConsumerFactory(Properties defaultProperties) {
        this(PropertiesUtils.asMap(defaultProperties));
    }

    @Override
    public MQPushConsumer create(String groupId,  Map<String, Object> overrideProperties) {
        String group = getGroupId(groupId, overrideProperties);
        PushConsumerProperties consumerProperties = new PushConsumerProperties(PropertiesUtils.asMap(getDefaultProperties(), overrideProperties));

        boolean aclEnabled = StringUtils.hasText(consumerProperties.getAccessKey()) && StringUtils.hasText(consumerProperties.getSecretKey());
        RPCHook rpcHook = aclEnabled ? new AclClientRPCHook(new SessionCredentials(consumerProperties.getAccessKey(), consumerProperties.getSecretKey())) : null;
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                group,
                rpcHook,
                consumerProperties.getAllocateMessageQueueStrategy(),
                Boolean.TRUE.equals(consumerProperties.getTraceEnabled()),
                consumerProperties.getCustomizedTraceTopic()
        );

        if (aclEnabled) {
            consumer.setVipChannelEnabled(false);
        }

        JavaUtils.INSTANCE
                .acceptIfHasText(consumerProperties.getNamespace(), consumer::setNamespace)
                .acceptIfHasText(consumerProperties.getInstanceName(), consumer::setInstanceName)
                .acceptIfHasText(consumerProperties.getNameServer(), consumer::setNamesrvAddr)
                .acceptIfNotNull(consumerProperties.getTlsEnabled(), consumer::setUseTLS)
                .acceptIfNotNull(consumerProperties.getAccessChannel(), consumer::setAccessChannel)
                .acceptIfNotNull(consumerProperties.getMessageModel(), consumer::setMessageModel)

                .acceptIfNotNull(consumerProperties.getMinConsumeThreads(), consumer::setConsumeThreadMin)
                .acceptIfNotNull(consumerProperties.getMaxConsumeThreads(), consumer::setConsumeThreadMax)
                .acceptIfNotNull(consumerProperties.getConsumeTimeoutMinutes(), consumer::setConsumeTimeout)
                .acceptIfNotNull(consumerProperties.getConsumeBatchSize(), consumer::setConsumeMessageBatchMaxSize)
                .acceptIfNotNull(consumerProperties.getPullBatchSize(), consumer::setPullBatchSize)
                .acceptIfNotNull(consumerProperties.getShutdownAwaitTerminationMillis(), consumer::setAwaitTerminationMillisWhenShutdown)
                .acceptIfNotNull(consumerProperties.getRetries(), consumer::setMaxReconsumeTimes);


        return consumer;
    }

}
