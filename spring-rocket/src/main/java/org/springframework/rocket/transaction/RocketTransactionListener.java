package org.springframework.rocket.transaction;

import org.springframework.messaging.Message;

public interface RocketTransactionListener extends RocketTransactionChecker {

    TransactionResolution execute(Message<?> message, Object arg);
}
