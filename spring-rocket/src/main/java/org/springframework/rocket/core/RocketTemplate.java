package org.springframework.rocket.core;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.consumer.LitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rocket.RocketException;
import org.springframework.rocket.client.ProducerProperties;
import org.springframework.rocket.client.RocketProducerFactory;
import org.springframework.rocket.client.RocketPullConsumerFactory;
import org.springframework.rocket.support.RocketHeaderUtils;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.support.RocketTransactionUtils;
import org.springframework.rocket.support.converter.DefaultMessagingMessageConverter;
import org.springframework.rocket.support.converter.MessagingMessageConverter;
import org.springframework.rocket.transaction.RocketTransactionListener;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Setter
public class RocketTemplate implements RocketOperations, RocketTransactionOperations, RocketReceivingOperations,
        ApplicationContextAware, SmartInitializingSingleton, InitializingBean, DisposableBean {

    protected MessagingMessageConverter messageConverter = new DefaultMessagingMessageConverter();
    protected MessageQueueSelector messageQueueSelector = new SelectMessageQueueByHash();

    private final RocketProducerFactory producerFactory;
    private MQProducer producer;
    private Long sendTimeoutMillis = DEFAULT_SEND_TIMEOUT_MILLIS;

    private final Map<String, MQProducer> transactionalProducers = new ConcurrentHashMap<>();
    private ExecutorService transactionExecutor;

    private RocketPullConsumerFactory pullConsumerFactory;
    private final Map<String, LitePullConsumer> pullConsumers = new ConcurrentHashMap<>();
    private Long pollTimeoutMillis = DEFAULT_POLL_TIMEOUT_MILLIS;

    /**
     *  Whether to record observations
     */
//    private boolean observationEnabled;
//    private ObservationRegistry observationRegistry;
//    private RocketTemplateObservationConvention observationConvention;
    private ApplicationContext applicationContext;


    public void setSendTimeoutMillis(Long sendTimeoutMillis) {
        Assert.isTrue(sendTimeoutMillis != null && sendTimeoutMillis > 0, "send timeout must be positive number");
        this.sendTimeoutMillis = sendTimeoutMillis;
    }

    private long getSendTimeoutMillis(Long timeoutMillis) {
        return timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : this.sendTimeoutMillis;
    }

    public void setPollTimeoutMillis(Long pollTimeoutMillis) {
        Assert.isTrue(pollTimeoutMillis != null && pollTimeoutMillis > 0, "poll timeout must be positive number");
        this.pollTimeoutMillis = pollTimeoutMillis;
    }

    private long getPollTimeoutMillis(Long timeoutMillis) {
        return timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : this.pollTimeoutMillis;
    }

    @Override
    public void afterSingletonsInstantiated() {
//        if (!this.observationEnabled) {
//            log.debug(() -> "Observations are not enabled - not recording");
//            return;
//        }
//        if (this.applicationContext == null) {
//            log.warn(() -> "Observations enabled but application context null - not recording");
//            return;
//        }
//        this.observationRegistry = this.applicationContext.getBeanProvider(ObservationRegistry.class)
//                .getIfUnique(() -> this.observationRegistry);
//        this.observationConvention = this.applicationContext.getBeanProvider(RocketTemplateObservationConvention.class)
//                .getIfUnique(() -> this.observationConvention);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        if (this.producer != null) {
            this.producer.shutdown();
        }
        if (!ObjectUtils.isEmpty(this.transactionalProducers)) {
            this.transactionalProducers.values().forEach(MQProducer::shutdown);
            this.transactionalProducers.clear();
        }
        if (!ObjectUtils.isEmpty(this.pullConsumers)) {
            this.pullConsumers.values().forEach(LitePullConsumer::shutdown);
            this.pullConsumers.clear();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.producer = producerFactory.create();
        if (this.producer != null) {
            this.producer.start();
        }
    }

    protected final ExecutorService getRequiredTransactionExecutor() {
        Assert.state(this.transactionExecutor != null, "No 'transactionExecutor' set");
        return this.transactionExecutor;
    }

    protected final RocketPullConsumerFactory getRequiredPullConsumerFactory() {
        Assert.state(this.pullConsumerFactory != null, "No 'pullConsumerFactory' set");
        return this.pullConsumerFactory;
    }

    /**
     * --------------------    sync send    --------------------
     */
    public SendResult send(TopicTag topicTag, Object payload) {
        return send(topicTag, payload, null);
    }
    public SendResult send(TopicTag topicTag, Object payload, Long timeoutMillis) {
        return send(topicTag.topic(), payload, supplyHeaders(topicTag.tag()), timeoutMillis);
    }
    public SendResult send(TopicTag topicTag, Object payload, String shardingKey, Delay delay) {
        return send(topicTag, payload, shardingKey, delay, null);
    }
    public SendResult send(TopicTag topicTag, Object payload, String shardingKey, Delay delay, Long timeoutMillis) {
        return send(topicTag.topic(), payload, supplyHeaders(topicTag.tag(), shardingKey, delay), timeoutMillis);
    }
    public SendResult send(String topic, Object payload) {
        return send(topic, payload, (Long) null);
    }
    public SendResult send(String topic, Object payload, Long timeoutMillis) {
        return send(topic, payload, null, timeoutMillis);
    }
    public SendResult send(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier) {
        return send(topic, payload, headerSupplier, null);
    }
    public SendResult send(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier, Long timeoutMillis) {
        Message<?> message = buildMessage(payload, headerSupplier);
        return send(topic, message, timeoutMillis);
    }
    @SneakyThrows
    @Override
    public SendResult send(String topic, Message<?> message, Long timeoutMillis) {
        Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
        org.apache.rocketmq.common.message.Message rocketMessage = this.messageConverter.fromMessage(converted, topic);

        SendResult sendResult;
        Object shardingKey = RocketHeaders.find(message.getHeaders(), RocketHeaders.SHARDING_KEY);
        if (!ObjectUtils.isEmpty(shardingKey)) {
            sendResult = this.producer.send(rocketMessage, this.messageQueueSelector, shardingKey, getSendTimeoutMillis(timeoutMillis));
        } else {
            sendResult = this.producer.send(rocketMessage, getSendTimeoutMillis(timeoutMillis));
        }
        return sendResult;
    }
    public SendResult sendBatch(TopicTag topicTag, List<?> payloads) {
        return sendBatch(topicTag, payloads, null);
    }
    public SendResult sendBatch(TopicTag topicTag, List<?> payloads, Long timeoutMillis) {
        List<Message<?>> messages = new ArrayList<>();
        for (Object payload : payloads) {
            Message<?> message = buildMessage(payload, supplyHeaders(topicTag.tag()));
            messages.add(message);
        }
        return sendBatch(topicTag.topic(), messages, timeoutMillis);
    }
    @SneakyThrows
    @Override
    public <T extends Message<?>> SendResult sendBatch(String topic, List<T> messages, Long timeoutMillis) {
        List<org.apache.rocketmq.common.message.Message> rocketMessages = messages.stream()
                .map(message ->  {
                    Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
                    return this.messageConverter.fromMessage(converted, topic);
                }).collect(Collectors.toList());

        SendResult sendResult;
        Object shardingKey = RocketHeaders.find(messages.get(0).getHeaders(), RocketHeaders.SHARDING_KEY);
        if (!ObjectUtils.isEmpty(shardingKey)) {
            MessageQueue messageQueue = this.messageQueueSelector.select(this.producer.fetchPublishMessageQueues(topic), null, shardingKey);
            sendResult = this.producer.send(rocketMessages, messageQueue, getSendTimeoutMillis(timeoutMillis));
        } else {
            sendResult = this.producer.send(rocketMessages, getSendTimeoutMillis(timeoutMillis));
        }
        return sendResult;
    }
    /**
     * --------------------    async send    --------------------
     */
    public void sendAsync(TopicTag topicTag, Object payload, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topicTag, payload, null, sendConsumer);
    }
    public void sendAsync(TopicTag topicTag, Object payload, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topicTag.topic(), payload, supplyHeaders(topicTag.tag()), timeoutMillis, sendConsumer);
    }
    public void sendAsync(TopicTag topicTag, Object payload, String shardingKey, Delay delay, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topicTag, payload, shardingKey, delay, null, sendConsumer);
    }
    public void sendAsync(TopicTag topicTag, Object payload, String shardingKey, Delay delay, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topicTag.topic(), payload, supplyHeaders(topicTag.tag(), shardingKey, delay), timeoutMillis, sendConsumer);
    }
    public void sendAsync(String topic, Object payload, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topic, payload, (Long) null, sendConsumer);
    }
    public void sendAsync(String topic, Object payload, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topic, payload, null, timeoutMillis, sendConsumer);
    }
    public void sendAsync(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendAsync(topic, payload, headerSupplier, null, sendConsumer);
    }
    public void sendAsync(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        Message<?> message = buildMessage(payload, headerSupplier);
        sendAsync(topic, message, timeoutMillis, sendConsumer);
    }
    @SneakyThrows
    @Override
    public void sendAsync(String topic, Message<?> message, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
        org.apache.rocketmq.common.message.Message rocketMessage = this.messageConverter.fromMessage(converted, topic);

        SendCallback callback = new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                sendConsumer.accept(sendResult, null);
            }

            @Override
            public void onException(Throwable e) {
                sendConsumer.accept(null, e);
            }
        };

        Object shardingKey = RocketHeaders.find(message.getHeaders(), RocketHeaders.SHARDING_KEY);
        if (!ObjectUtils.isEmpty(shardingKey)) {
            this.producer.send(rocketMessage, this.messageQueueSelector, shardingKey, callback, getSendTimeoutMillis(timeoutMillis));
        } else {
            this.producer.send(rocketMessage, callback, getSendTimeoutMillis(timeoutMillis));
        }
    }
    public void sendBatchAsync(TopicTag topicTag, List<?> payloads, BiConsumer<SendResult, Throwable> sendConsumer) {
        sendBatchAsync(topicTag, payloads, null, sendConsumer);
    }
    public void sendBatchAsync(TopicTag topicTag, List<?> payloads, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        List<Message<?>> messages = new ArrayList<>();
        for (Object payload : payloads) {
            Message<?> message = buildMessage(payload, supplyHeaders(topicTag.tag()));
            messages.add(message);
        }
        sendBatchAsync(topicTag.topic(), messages, timeoutMillis, sendConsumer);
    }
    @SneakyThrows
    @Override
    public <T extends Message<?>> void sendBatchAsync(String topic, List<T> messages, Long timeoutMillis, BiConsumer<SendResult, Throwable> sendConsumer) {
        List<org.apache.rocketmq.common.message.Message> rocketMessages = messages.stream()
                .map(message -> {
                    Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
                    return this.messageConverter.fromMessage(converted, topic);
                }).collect(Collectors.toList());

        SendCallback callback = new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                sendConsumer.accept(sendResult, null);
            }

            @Override
            public void onException(Throwable e) {
                sendConsumer.accept(null, e);
            }
        };

        Object shardingKey = RocketHeaders.find(messages.get(0).getHeaders(), RocketHeaders.SHARDING_KEY);
        if (!ObjectUtils.isEmpty(shardingKey)) {
            MessageQueue messageQueue = this.messageQueueSelector.select(this.producer.fetchPublishMessageQueues(topic), null, shardingKey);
            this.producer.send(rocketMessages, messageQueue, callback, getSendTimeoutMillis(timeoutMillis));
        } else {
            this.producer.send(rocketMessages, callback, getSendTimeoutMillis(timeoutMillis));
        }
    }
    /**
     * --------------------    send oneway    --------------------
     */
    public void sendOneway(TopicTag topicTag, Object payload) {
        sendOneway(topicTag, payload, null);
    }
    public void sendOneway(TopicTag topicTag, Object payload, String shardingKey) {
        sendOneway(topicTag.topic(), payload, supplyHeaders(topicTag.tag(), shardingKey, null));
    }
    public void sendOneway(String topic, Object payload) {
        sendOneway(topic, payload, null);
    }
    public void sendOneway(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier) {
        Message<?> message = buildMessage(payload, headerSupplier);
        sendOneway(topic, message);
    }
    @SneakyThrows
    @Override
    public void sendOneway(String topic, Message<?> message) {
        Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
        org.apache.rocketmq.common.message.Message rocketMessage = this.messageConverter.fromMessage(converted, topic);

        Object shardingKey = RocketHeaders.find(message.getHeaders(), RocketHeaders.SHARDING_KEY);
        if (!ObjectUtils.isEmpty(shardingKey)) {
            this.producer.sendOneway(rocketMessage, this.messageQueueSelector, shardingKey);
        } else {
            this.producer.sendOneway(rocketMessage);
        }
    }
    /**
     * --------------------    transaction    --------------------
     */
    public TransactionSendResult sendInTransaction(TopicTag topicTag, Object payload, Object arg) {
        return sendInTransaction(topicTag.topic(), payload, () -> {
            Map<String, Object> headers = new HashMap<>(64);
            RocketHeaderUtils.TAGS_HEADER_SET.accept(headers, topicTag.tag());
            RocketHeaderUtils.TRANSACTION_ARG_HEADER_SET.accept(headers, arg);
            return headers;
        });
    }
    public TransactionSendResult sendInTransaction(String topic, Object payload, Object arg) {
        return sendInTransaction(topic, payload, () -> {
            Map<String, Object> headers = new HashMap<>(64);
            RocketHeaderUtils.TRANSACTION_ARG_HEADER_SET.accept(headers, arg);
            return headers;
        });
    }
    public TransactionSendResult sendInTransaction(String topic, Object payload, Supplier<Map<String, Object>> headerSupplier) {
        Message<?> message = buildMessage(payload, headerSupplier);
        return sendInTransaction(topic, message);
    }
    @SneakyThrows
    @Override
    public TransactionSendResult sendInTransaction(String topic, Message<?> message) {
        MQProducer producer = this.transactionalProducers.get(topic);
        Assert.state(producer instanceof TransactionMQProducer, String.format("No producer for %s or producer does not support transactions", topic));

        Message<?> converted = this.messageConverter.convert(message.getPayload(), message.getHeaders());
        org.apache.rocketmq.common.message.Message rocketMessage = this.messageConverter.fromMessage(converted, topic);

        Object transactionArg = RocketHeaders.find(message.getHeaders(), RocketHeaders.TRANSACTION_ARG);
        return producer.sendMessageInTransaction(rocketMessage, transactionArg);
    }

    public void registerTransactional(String topic, RocketTransactionListener transactionListener) {
        registerTransactional(topic, transactionListener, null);
    }

    public void registerTransactional(String topic, RocketTransactionListener transactionListener, Map<String, Object> producerProperties) {
        doRegisterTransactional(topic, transactionListener, producerProperties);
    }

    private MQProducer doRegisterTransactional(String topic, RocketTransactionListener transactionListener, Map<String, Object> producerProperties) {
        Assert.notNull(transactionListener, "transactionListener must not be null");

        Map<String, Object> transactionalProperties = producerProperties == null ? new HashMap<>(64) : producerProperties;
        transactionalProperties.put(ProducerProperties.TRANSACTIONAL, true);
        return this.transactionalProducers.computeIfAbsent(topic, key -> {
            MQProducer producer = this.producerFactory.create(transactionalProperties);

            if (producer instanceof TransactionMQProducer transactionProducer) {
                transactionProducer.setTransactionListener(RocketTransactionUtils.toTransactionListener(transactionListener, this.messageConverter::toMessage));
                transactionProducer.setExecutorService(getRequiredTransactionExecutor());
                try {
                    transactionProducer.start();
                } catch (MQClientException e) {
                    throw new RocketException(e.getErrorMessage(), e);
                }
            }

            return producer;
        });
    }
    /**
     * --------------------    receive    --------------------
     */
    @Override
    public List<MessageExt> receive(String topic, Long timeoutMillis) {
        return receive(new TopicTag(topic, null), timeoutMillis);
    }
    public List<MessageExt> receive(TopicTag topicTag) {
        return receive(topicTag, null);
    }
    public List<MessageExt> receive(TopicTag topicTag, Long timeoutMillis) {
        LitePullConsumer consumer = doSubscribe(topicTag);
        return consumer.poll(getPollTimeoutMillis(timeoutMillis));
    }
    @Override
    public void receiveAsync(String topic, Long timeoutMillis, BiConsumer<List<MessageExt>, Throwable> receiveConsumer) {
        receiveAsync(new TopicTag(topic, null), timeoutMillis, receiveConsumer);
    }
    public void receiveAsync(TopicTag topicTag, BiConsumer<List<MessageExt>, Throwable> receiveConsumer) {
        receiveAsync(topicTag, null, receiveConsumer);
    }
    public void receiveAsync(TopicTag topicTag, Long timeoutMillis, BiConsumer<List<MessageExt>, Throwable> receiveConsumer) {
        throw new UnsupportedOperationException("RocketTemplate doesn't support receiveAsync");
    }
    @Override
    public <T> List<T> receiveAndConvert(String topic, Type payloadType, Long timeoutMillis) {
        return receiveAndConvert(new TopicTag(topic, null), payloadType, timeoutMillis);
    }
    public <T> List<T> receiveAndConvert(TopicTag topicTag, Type payloadType) {
        return receiveAndConvert(topicTag, payloadType, null);
    }
    public <T> List<T> receiveAndConvert(TopicTag topicTag, Type payloadType, Long timeoutMillis) {
        List<MessageExt> rocketMessages = receive(topicTag, timeoutMillis);
        return convert(rocketMessages, payloadType);
    }
    @Override
    public <T> void receiveAndConvertAsync(String topic, Type payloadType, Long timeoutMillis, BiConsumer<List<T>, Throwable> receiveConsumer) {
        receiveAndConvertAsync(new TopicTag(topic, null), payloadType, timeoutMillis, receiveConsumer);
    }
    public <T> void receiveAndConvertAsync(TopicTag topicTag, Type payloadType, BiConsumer<List<T>, Throwable> receiveConsumer) {
        receiveAndConvertAsync(topicTag, payloadType, null, receiveConsumer);
    }
    public <T> void receiveAndConvertAsync(TopicTag topicTag, Type payloadType, Long timeoutMillis, BiConsumer<List<T>, Throwable> receiveConsumer) {
        receiveAsync(topicTag, timeoutMillis, (messageViews, throwable) -> receiveConsumer.accept(convert(messageViews, payloadType), throwable));
    }

    public void subscribe(TopicTag... topicTags) {
        Arrays.stream(topicTags).forEach(this::doSubscribe);
    }
    private LitePullConsumer doSubscribe(TopicTag topicTag) {
        return this.pullConsumers.computeIfAbsent(topicTag.toString(), key -> {
            LitePullConsumer consumer = getRequiredPullConsumerFactory().create();
            try {
                consumer.subscribe(topicTag.topic(), topicTag.tag());
                consumer.start();
            } catch (MQClientException e) {
                throw new RocketException(e.getErrorMessage(), e);
            }
            return consumer;
        });
    }
    @SuppressWarnings("unchecked")
    private <T> List<T> convert(List<MessageExt> messages, Type type) {
        if (ObjectUtils.isEmpty(messages)) {
            return new ArrayList<>();
        }
        return messages.stream().map(rocketMessage -> (T) this.messageConverter.toMessage(rocketMessage, type).getPayload()).toList();
    }

    private Message<?> buildMessage(Object payload, Supplier<Map<String, Object>> headerSupplier) {
        Message<?> message;
        if (headerSupplier == null || ObjectUtils.isEmpty(headerSupplier.get())) {
            message = MessageBuilder.withPayload(payload).build();
        } else {
            message = MessageBuilder.withPayload(payload).copyHeaders(headerSupplier.get()).build();
        }
        return message;
    }

    private Supplier<Map<String, Object>> supplyHeaders(String tags) {
        return () -> {
            Map<String, Object> headers = new HashMap<>(64);
            RocketHeaderUtils.TAGS_HEADER_SET.accept(headers, tags);
            return headers;
        };
    }

    private Supplier<Map<String, Object>> supplyHeaders(String tags, String shardingKey, Delay delay) {
        return () -> {
            Map<String, Object> headers = new HashMap<>(64);
            RocketHeaderUtils.TAGS_HEADER_SET.accept(headers, tags);
            RocketHeaderUtils.SHARDING_KEY_HEADER_SET.accept(headers, shardingKey);
            RocketHeaderUtils.DELAY_HEADER_SET.accept(headers, delay);
            return headers;
        };
    }
}
