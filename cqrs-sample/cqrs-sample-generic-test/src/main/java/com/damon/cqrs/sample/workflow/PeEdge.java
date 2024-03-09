package com.damon.cqrs.sample.workflow;

import lombok.Data;

@Data
public class PeEdge {
    public String id;
    public PeNode from;
    public PeNode to;

    public PeEdge(String id) {
        this.id = id;
    }
}