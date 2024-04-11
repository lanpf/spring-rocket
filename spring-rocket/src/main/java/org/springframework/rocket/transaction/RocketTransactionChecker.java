package org.springframework.rocket.transaction;

import org.springframework.messaging.Message;

public interface RocketTransactionChecker {

    TransactionResolution check(Message<?> message);
}
