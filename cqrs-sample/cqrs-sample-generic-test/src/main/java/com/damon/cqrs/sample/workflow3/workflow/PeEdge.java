package com.damon.cqrs.sample.workflow3.workflow;

import lombok.Data;

@Data
public class PeEdge {
    private String id;
    private PeNode from;
    private PeNode to;
    private String expandInfo;

    public PeEdge(String id, String expandInfo) {
        this.id = id;
        this.expandInfo = expandInfo;
    }

    public PeEdge(String id) {
        this.id = id;
    }
}