package org.springframework.rocket.core;

import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.messaging.Message;

public interface RocketTransactionOperations {

    /**
     * send spring message in transaction
     */
    TransactionSendResult sendInTransaction(String topic, Message<?> message);
}
