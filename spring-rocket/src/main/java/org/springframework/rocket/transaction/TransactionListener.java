package org.springframework.rocket.transaction;

import org.springframework.messaging.Message;

public interface TransactionListener {

    TransactionState execute(Message<?> message, Object arg);

    TransactionState check(Message<?> message);
}
