package org.springframework.rocket;

public class RocketException extends RuntimeException {

    public RocketException(String message) {
        super(message);
    }

    public RocketException(String message, Throwable cause) {
        super(message, cause);
    }
}
