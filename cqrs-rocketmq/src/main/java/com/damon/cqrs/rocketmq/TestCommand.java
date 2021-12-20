package com.damon.cqrs.rocketmq;

import com.damon.cqrs.domain.Command;

public class TestCommand extends Command{

    /**
     * 
     */
    private static final long serialVersionUID = 3534595230983262257L;

    public TestCommand(long commandId, long aggregateId) {
        super(commandId, aggregateId);
        // TODO Auto-generated constructor stub
    }

}
