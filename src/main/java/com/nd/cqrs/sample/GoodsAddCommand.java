package com.nd.cqrs.sample;

import com.nd.cqrs.domain.Command;

public class GoodsAddCommand extends Command{

    private String name;
    private int count;
    
    public GoodsAddCommand(long commandId, long aggregateId,String name, int count) {
        super(commandId, aggregateId);
        // TODO Auto-generated constructor stub
        this.name = name;
        this.count  =count;
        
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    
    
    
    

}
