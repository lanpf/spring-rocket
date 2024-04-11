package org.springframework.rocket.transaction;

public enum TransactionResolution {
    /**
     * @see org.apache.rocketmq.client.producer.LocalTransactionState#COMMIT_MESSAGE
     */
    COMMIT,
    /**
     * @see org.apache.rocketmq.client.producer.LocalTransactionState#ROLLBACK_MESSAGE
     */
    ROLLBACK,
    /**
     * @see org.apache.rocketmq.client.producer.LocalTransactionState#UNKNOW
     */
    UNKNOWN
}
