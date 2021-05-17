package com.damon.cqrs.exception;

public class AggregateCommandConflictException extends RuntimeException {

    private long aggregateId;

    private String aggregateType;

    private long commandId;

    /**
     * 
     */
    private static final long serialVersionUID = -6513851101874096469L;

    /**
     * 
     */
    public AggregateCommandConflictException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public AggregateCommandConflictException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public AggregateCommandConflictException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public AggregateCommandConflictException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public AggregateCommandConflictException(long aggregateId, String aggregateType, long commandId, Throwable cause) {
        super(cause);
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.commandId = commandId;
        // TODO Auto-generated constructor stub
    }

    public long getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public long getCommandId() {
        return commandId;
    }

}
