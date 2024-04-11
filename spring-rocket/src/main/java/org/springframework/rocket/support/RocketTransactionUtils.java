package org.springframework.rocket.support;

import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.rocket.transaction.RocketTransactionListener;
import org.springframework.rocket.transaction.TransactionResolution;

import java.util.function.Function;

public class RocketTransactionUtils {

    private static org.apache.rocketmq.client.producer.LocalTransactionState toLocalTransactionState(TransactionResolution transactionResolution) {
        return switch (transactionResolution) {
            case COMMIT -> org.apache.rocketmq.client.producer.LocalTransactionState.COMMIT_MESSAGE;
            case ROLLBACK -> org.apache.rocketmq.client.producer.LocalTransactionState.ROLLBACK_MESSAGE;
            default -> org.apache.rocketmq.client.producer.LocalTransactionState.UNKNOW;
        };
    }

    public static TransactionListener toTransactionListener(RocketTransactionListener transactionListener, Function<Message, org.springframework.messaging.Message<?>> messageConverter) {
        return new TransactionListener() {
            @Override
            public org.apache.rocketmq.client.producer.LocalTransactionState executeLocalTransaction(Message rocketMessage, Object arg) {
                return toLocalTransactionState(transactionListener.execute(messageConverter.apply(rocketMessage), arg));
            }

            @Override
            public org.apache.rocketmq.client.producer.LocalTransactionState checkLocalTransaction(MessageExt rocketMessage) {
                return toLocalTransactionState(transactionListener.check(messageConverter.apply(rocketMessage)));
            }
        };
    }

}