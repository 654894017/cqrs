package com.damon.cqrs.rocketmq.core;

public enum CommandReturnType {

    /**
     * Return the command result when the command execution has the following cases:
     * 1. the command execution meets some error or exception;
     * 2. the command execution makes nothing changes of domain;
     * 3. the command execution success, and the domain event is sent to the message queue successfully.
     */
    CommandExecuted(1),

    /**
     * Return the command result when the command execution has the following cases:
     * 1. the command execution meets some error or exception;
     * 2. the command execution makes nothing changes of domain;
     * 3. the command execution success, and the domain event is handled.
     */
    EventHandled(2);

    private int type;

    private CommandReturnType(int type) {
        this.type = type;

    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


}