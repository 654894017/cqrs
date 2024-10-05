package com.damon.cqrs.sample.workflow2.workflow;

import lombok.Data;

@Data
public class PeProcess {
    private String id;
    private PeNode start;

    public PeProcess(String id, PeNode start) {
        this.id = id;
        this.start = start;
    }


    public void applyStartedProcess() {

    }

//    @Override
//    public void setId(Long id) {
//        this.id = "a1";
//    }
}
 

 
