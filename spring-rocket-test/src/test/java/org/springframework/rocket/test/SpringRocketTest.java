package org.springframework.rocket.test;

import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.rocket.core.Delay;
import org.springframework.rocket.core.DelayMode;
import org.springframework.rocket.core.RocketTemplate;
import org.springframework.rocket.core.TopicTag;
import org.springframework.rocket.support.RocketHeaders;
import org.springframework.rocket.test.dto.PayloadSend;
import org.springframework.rocket.test.util.MapBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
@SpringJUnitConfig
@SpringBootTest(classes = SpringRocketApplication.class, properties = {
        "rocket.properties.traceEnabled=true",
        "rocket.name-server=127.0.0.1:9876",
        "rocket.producer.group_id=GID_SAMPLES_MESSAGE",
        "rocket.consumer.push.message-model=CLUSTERING",
        "rocket.consumer.push.pull-batch-size=32",
        "rocket.consumer.pull.group_id=GID_SAMPLES_MESSAGE_POLLER",
        "rocket.consumer.pull.message-model=CLUSTERING",
        "rocket.consumer.pull.pull-batch-size=15"
})
public class SpringRocketTest {

    @Resource
    private RocketTemplate rocketTemplate;

    private static final Function<String, BiConsumer<SendResult, Throwable>> SEND_CONSUMER = prefix -> (sendResult, e) -> {
        if (e == null) {
            log.info(prefix + " success, result: {}", sendResult);
        } else {
            log.error(prefix + " fail", e);
        }
    };

    @SneakyThrows
    @Test
    public void sendTest() {
        String topic = "rocket-send-simple";

        SendResult sendResult;

        sendResult = rocketTemplate.send(topic, PayloadSend.create());
        log.info("sync send simple: {}", sendResult);

        sendResult = rocketTemplate.send(new TopicTag(topic, "1"), PayloadSend.create());
        log.info("sync send simple with tags: {}", sendResult);

        Map<String, Object> headers = MapBuilder.builder()
                .put(RocketHeaders.KEYS, "keys-rocket-send-simple")
                .build();
        sendResult = rocketTemplate.send(topic, PayloadSend.create(), () -> headers);
        log.info("sync send simple with header: {}", sendResult);

        sendResult = rocketTemplate.send(topic, MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers)));
        log.info("sync send simple messaging: {}", sendResult);

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendBatchTest() {
        String topic = "rocket-send-batch";

        SendResult sendResult;

        List<PayloadSend> payloads = IntStream.range(0, 20).boxed().map(i -> PayloadSend.create()).toList();
        sendResult = rocketTemplate.sendBatch(TopicTag.of(topic), payloads);
        log.info("sync send batch simple: {}", sendResult);

        List<Message<PayloadSend>> messages = IntStream.range(0, 20).boxed().map(i -> {
            Map<String, Object> headers = MapBuilder.builder()
                    .put(RocketHeaders.KEYS, "keys-rocket-send-batch")
                    .build();
            return MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers));
        }).toList();
        sendResult = rocketTemplate.sendBatch(topic, messages);
        log.info("sync send batch simple messaging: {}", sendResult);

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendDelayTest() {
        String topic = "rocket-send-delay";

        SendResult sendResult;

        sendResult = rocketTemplate.send(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELIVER_TIME_MILLIS, System.currentTimeMillis() + 1000L));
        log.info("sync send delay with deliver time millis: {}", sendResult);

        sendResult = rocketTemplate.send(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_MILLIS, 1000L));
        log.info("sync send delay with delay millis: {}", sendResult);

        sendResult = rocketTemplate.send(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_SECONDS, 1L));
        log.info("sync send delay with delay seconds: {}", sendResult);

        sendResult = rocketTemplate.send(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_LEVEL, 1L));
        log.info("sync send delay with delay level: {}", sendResult);

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendSequentialTest() {
        String topic = "rocket-send-sequential";

        IntStream.range(0, 5).boxed().forEach(i -> {
            SendResult sendResult = rocketTemplate.send(TopicTag.of(topic), PayloadSend.create(String.valueOf(i)), "sharding-rocket-send-sequential", null);
            log.info("sync send sequential with sharding key: {}", sendResult);
        });

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendAsyncTest() {
        String topic = "rocket-send-simple";

        rocketTemplate.sendAsync(topic, PayloadSend.create(),
                SEND_CONSUMER.apply("async send simple"));

        rocketTemplate.sendAsync(new TopicTag(topic, "1"), PayloadSend.create(),
                SEND_CONSUMER.apply("async send simple with tags"));

        Map<String, Object> headers = MapBuilder.builder()
                .put(RocketHeaders.KEYS, "keys-rocket-send-simple")
                .build();
        rocketTemplate.sendAsync(topic, PayloadSend.create(), () -> headers,
                SEND_CONSUMER.apply("async send simple with header"));

        rocketTemplate.sendAsync(topic, MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers)),
                SEND_CONSUMER.apply("async send simple messaging"));

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendBatchAsyncTest() {
        String topic = "rocket-send-batch";

        List<PayloadSend> payloads = IntStream.range(0, 20).boxed().map(i -> PayloadSend.create()).toList();
        rocketTemplate.sendBatchAsync(TopicTag.of(topic), payloads,
                SEND_CONSUMER.apply("async send batch simple"));

        List<Message<PayloadSend>> messages = IntStream.range(0, 20).boxed().map(i -> {
            Map<String, Object> headers = MapBuilder.builder()
                    .put(RocketHeaders.KEYS, "keys-rocket-send-batch")
                    .build();
            return MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers));
        }).toList();
        rocketTemplate.sendBatchAsync(topic, messages,
                SEND_CONSUMER.apply("async send batch simple messaging"));

        Thread.sleep(5000);
    }


    @SneakyThrows
    @Test
    public void sendDelayAsyncTest() {
        String topic = "rocket-send-delay";

        rocketTemplate.sendAsync(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELIVER_TIME_MILLIS, System.currentTimeMillis() + 1000L),
                SEND_CONSUMER.apply("async send delay with deliver time millis"));

        rocketTemplate.sendAsync(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_MILLIS, 1000L),
                SEND_CONSUMER.apply("async send delay with delay millis"));

        rocketTemplate.sendAsync(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_SECONDS, 1L),
                SEND_CONSUMER.apply("async send delay with delay seconds"));

        rocketTemplate.sendAsync(TopicTag.of(topic), PayloadSend.create(), null, new Delay(DelayMode.DELAY_LEVEL, 1L),
                SEND_CONSUMER.apply("async send delay with delay level"));

        Thread.sleep(5000);
    }

    @SneakyThrows
    @Test
    public void sendSequentialAsyncTest() {
        String topic = "rocket-send-sequential";

        doSendSequentialAsync(topic, 0);

        Thread.sleep(5000);
    }

    private void doSendSequentialAsync(String topic, int i) {
        if (i >= 5) {
            return;
        }
        rocketTemplate.sendAsync(TopicTag.of(topic), PayloadSend.create(String.valueOf(i)), "sharding-rocket-send-sequential", null,
                (sendResult, e) -> {
                    String prefix = "async send sequential with sharding key";
                    if (e == null) {
                        log.info(prefix + " success, result: {}", sendResult);
                    } else {
                        log.error(prefix + " fail", e);
                    }
                    doSendSequentialAsync(topic, i + 1);
                });
    }

    @SneakyThrows
    @Test
    public void sendOnewayTest() {
        String topic = "rocket-send-oneway";

        rocketTemplate.sendOneway(topic, PayloadSend.create());

        rocketTemplate.sendOneway(new TopicTag(topic, "1"), PayloadSend.create());

        Map<String, Object> headers = MapBuilder.builder()
                .put(RocketHeaders.KEYS, "keys-rocket-send-oneway")
                .build();
        rocketTemplate.sendOneway(topic, PayloadSend.create(), () -> headers);

        rocketTemplate.sendOneway(topic, MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers)));

        IntStream.range(0, 5).boxed().forEach(i -> rocketTemplate.sendOneway(TopicTag.of(topic), PayloadSend.create(String.valueOf(i)), "sharding-rocket-send-oneway"));

        Thread.sleep(5000);
    }

    @SneakyThrows
    @Test
    public void sendInTransactionTest() {
        String topic = "rocket-send-transaction";

        TransactionSendResult sendResult;
        sendResult = rocketTemplate.sendInTransaction(topic, PayloadSend.create(), randomTransactionArg());
        log.info("send in transaction: {}", sendResult);

        sendResult = rocketTemplate.sendInTransaction(new TopicTag(topic, "1"), PayloadSend.create(), randomTransactionArg());
        log.info("send in transaction with tags: {}", sendResult);

        Map<String, Object> headers = MapBuilder.builder()
                .put(RocketHeaders.KEYS, "keys-rocket-send-transaction")
                .put(RocketHeaders.TRANSACTION_ARG, randomTransactionArg())
                .build();

        sendResult = rocketTemplate.sendInTransaction(topic, PayloadSend.create(), () -> headers);
        log.info("send in transaction with header: {}", sendResult);

        sendResult = rocketTemplate.sendInTransaction(topic, MessageBuilder.createMessage(PayloadSend.create(), new MessageHeaders(headers)));
        log.info("send in transaction messaging: {}", sendResult);

        Thread.sleep(10000);
    }

    private Object randomTransactionArg() {
        return ((int) (Math.random() * 100)) % 2 == 0;
    }

    @SneakyThrows
    @Test
    public void receiveTest() {
        String topic = "rocket-receive";

        List<PayloadSend> payloads = IntStream.range(0, 100).boxed().map(i -> PayloadSend.create()).toList();
        SendResult sendResult = rocketTemplate.sendBatch(TopicTag.of(topic), payloads);
        log.info("sync send prepared for receive: {}", sendResult);

        Executors.newScheduledThreadPool(5).scheduleAtFixedRate(() -> {
            List<MessageExt> receiveRocketMessages = rocketTemplate.receive(TopicTag.of(topic));
            if (!ObjectUtils.isEmpty(receiveRocketMessages)) {
                log.info("receive {} messages", receiveRocketMessages.size());
                receiveRocketMessages.forEach(message -> log.info("receive {} message: {}, queue: {}", topic, message, message.getQueueId()));
            } else {
                log.info("receive no messages");
            }
        }, -1, 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(10000);
    }


    @SneakyThrows
    @Test
    public void receiveAndConvertTest() {
        String topic = "rocket-receive-convert";

        List<PayloadSend> payloads = IntStream.range(0, 100).boxed().map(i -> PayloadSend.create()).toList();
        SendResult sendResult = rocketTemplate.sendBatch(TopicTag.of(topic), payloads);
        log.info("sync send prepared for receive: {}", sendResult);

        Executors.newScheduledThreadPool(5).scheduleAtFixedRate(() -> {
            List<PayloadSend> receivePayloads = rocketTemplate.receiveAndConvert(TopicTag.of(topic), PayloadSend.class);
            if (!ObjectUtils.isEmpty(receivePayloads)) {
                log.info("receive {} payloads", receivePayloads.size());
                receivePayloads.forEach(payload -> log.info("receive {} payload: {}", topic, payload));
            } else {
                log.info("receive no payloads");
            }
        }, -1, 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(10000);
    }


    @SneakyThrows
    @Test
    public void consumerTest() {
        Thread.sleep(5000);
    }
}
