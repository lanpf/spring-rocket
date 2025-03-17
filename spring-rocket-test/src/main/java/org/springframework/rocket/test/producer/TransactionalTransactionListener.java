package org.springframework.rocket.test.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.rocket.annotation.RocketTransactionListener;
import org.springframework.rocket.transaction.TransactionListener;
import org.springframework.rocket.transaction.TransactionState;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketTransactionListener(topic = "rocket-send-transaction")
public class TransactionalTransactionListener implements TransactionListener {

    /**
     * payload will always bytes.
     */
    @Override
    public TransactionState execute(Message<?> message, Object arg) {
        log.info("execute local transaction. message: {}, arg: {}", message, arg);
        try {
            boolean result = (boolean) arg;
            if (result) {
                log.info("local transaction commit success.");
                return TransactionState.COMMIT;
            } else {
                log.info("local transaction commit failed. message dropped.");
                return TransactionState.ROLLBACK;
            }
        } catch (Throwable e) {
            // rocket will check local transaction status
            log.info("local transaction status is unknown, will check it later.");
            return TransactionState.UNKNOWN;
        }
    }

    /**
     * local transaction status check interval: 60 seconds
     * first time check:                        the value of local transaction status check interval
     * timeout:                                 4 hours
     */
    @Override
    public TransactionState check(Message<?> message) {
        log.info("local transaction commit success after check. message: {}", message);
        return TransactionState.COMMIT;
    }
}
