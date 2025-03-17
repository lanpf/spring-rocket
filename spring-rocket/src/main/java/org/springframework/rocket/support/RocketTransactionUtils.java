package org.springframework.rocket.support;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.rocket.transaction.TransactionListener;
import org.springframework.rocket.transaction.TransactionState;

import java.util.function.Function;

public class RocketTransactionUtils {

    private static org.apache.rocketmq.client.producer.LocalTransactionState translate(TransactionState transactionState) {
        return switch (transactionState) {
            case COMMIT -> org.apache.rocketmq.client.producer.LocalTransactionState.COMMIT_MESSAGE;
            case ROLLBACK -> org.apache.rocketmq.client.producer.LocalTransactionState.ROLLBACK_MESSAGE;
            default -> org.apache.rocketmq.client.producer.LocalTransactionState.UNKNOW;
        };
    }

    public static org.apache.rocketmq.client.producer.TransactionListener translate(TransactionListener transactionListener, Function<Message, org.springframework.messaging.Message<?>> messageConverter) {
        return new org.apache.rocketmq.client.producer.TransactionListener() {
            @Override
            public org.apache.rocketmq.client.producer.LocalTransactionState executeLocalTransaction(Message rocketMessage, Object arg) {
                return translate(transactionListener.execute(messageConverter.apply(rocketMessage), arg));
            }

            @Override
            public org.apache.rocketmq.client.producer.LocalTransactionState checkLocalTransaction(MessageExt rocketMessage) {
                return translate(transactionListener.check(messageConverter.apply(rocketMessage)));
            }
        };
    }

}