package com.damon.cqrs.event;

public enum EventAppendStatus {
    Success, DuplicateEvent, DuplicateCommand, Exception
}
