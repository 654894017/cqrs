package com.damon.cqrs.exception;

public class EventQueryException extends RuntimeException {
    public EventQueryException(String message) {
        super(message);
    }

    public EventQueryException(Throwable cause) {
        super(cause);
    }
}
