package org.springframework.rocket.test.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.rocket.annotation.RocketTransactional;
import org.springframework.rocket.transaction.RocketTransactionListener;
import org.springframework.rocket.transaction.TransactionResolution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketTransactional(topic = "rocket-send-transaction")
public class RocketTransactionalTransactionListener implements RocketTransactionListener {

    /**
     * payload will always bytes.
     */
    @Override
    public TransactionResolution execute(Message<?> message, Object arg) {
        log.info("execute local transaction. message: {}, arg: {}", message, arg);
        try {
            boolean result = (boolean) arg;
            if (result) {
                log.info("local transaction commit success.");
                return TransactionResolution.COMMIT;
            } else {
                log.info("local transaction commit failed. message dropped.");
                return TransactionResolution.ROLLBACK;
            }
        } catch (Throwable e) {
            // rocket will check local transaction status
            log.info("local transaction status is unknown, will check it later.");
            return TransactionResolution.UNKNOWN;
        }
    }

    /**
     * local transaction status check interval: 60 seconds
     * first time check:                        the value of local transaction status check interval
     * timeout:                                 4 hours
     */
    @Override
    public TransactionResolution check(Message<?> message) {
        log.info("local transaction commit success after check. message: {}", message);
        return TransactionResolution.COMMIT;
    }
}
