package com.damon.cqrs.sample.workflow;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class PeNode {
    public String id;
    public String type;
    public List<PeEdge> in = new ArrayList<>();
    public List<PeEdge> out = new ArrayList<>();
    public Node xmlNode;

    public PeNode(String id) {
        this.id = id;
    }

    public PeEdge onlyOneOut() {
        return out.get(0);
    }

    public PeEdge outWithID(String nextPeEdgeID) {
        return out.stream().filter(e -> e.id.equals(nextPeEdgeID)).findFirst().get();
    }

    public PeEdge outWithOutID(String nextPeEdgeID) {
        return out.stream().filter(e -> !e.id.equals(nextPeEdgeID)).findFirst().get();
    }

}