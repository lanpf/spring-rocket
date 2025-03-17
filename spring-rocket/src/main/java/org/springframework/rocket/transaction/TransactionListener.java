package org.springframework.rocket.transaction;

import org.springframework.messaging.Message;

public interface TransactionListener extends TransactionChecker {

    TransactionState execute(Message<?> message, Object arg);
}
