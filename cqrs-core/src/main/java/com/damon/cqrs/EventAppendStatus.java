package com.damon.cqrs;

public enum EventAppendStatus {
    Success, DuplicateEvent, DuplicateCommand, Exception
}
