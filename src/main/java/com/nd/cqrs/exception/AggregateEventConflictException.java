package com.nd.cqrs.exception;

public class AggregateEventConflictException extends RuntimeException{

    /**
     * 
     */
    private static final long serialVersionUID = -6513851101874096469L;

    /**
     * 
     */
    public AggregateEventConflictException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public AggregateEventConflictException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     * @param cause
     */
    public AggregateEventConflictException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message
     */
    public AggregateEventConflictException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     */
    public AggregateEventConflictException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }
    
    

}
