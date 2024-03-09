package com.damon.cqrs.sample.workflow;

import lombok.Data;

@Data
public class PeProcess {
    public String id;
    public PeNode start;

    public PeProcess(String id, PeNode start) {
        this.id = id;
        this.start = start;
    }
}
 

 
