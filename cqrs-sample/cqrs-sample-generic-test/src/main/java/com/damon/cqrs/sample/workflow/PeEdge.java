package com.damon.cqrs.sample.workflow;

import lombok.Data;

@Data
public class PeEdge {
    public String id;
    public PeNode from;
    public PeNode to;
    public String expandJson;

    public PeEdge(String id, String expandJson) {
        this.id = id;
        this.expandJson = expandJson;
    }

    public PeEdge(String id) {
        this.id = id;
    }
}