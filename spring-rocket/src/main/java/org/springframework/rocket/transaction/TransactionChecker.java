package org.springframework.rocket.transaction;

import org.springframework.messaging.Message;

public interface TransactionChecker {

    TransactionState check(Message<?> message);
}
