package com.nd.cqrs;

public enum EventAppendStatus {
    Success, DuplicateEvent, DuplicateCommand, Exception
}
