package com.damon.cqrs.exception;

public class EventOfffsetUpdateException extends RuntimeException {
    public EventOfffsetUpdateException(String message) {
        super(message);
    }

    public EventOfffsetUpdateException(Throwable cause) {
        super(cause);
    }
}
