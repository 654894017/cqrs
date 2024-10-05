package com.damon.cqrs.sample.workflow3.workflow;

import java.util.ArrayList;
import java.util.List;

public class PeNode {
    private String id;
    private String type;
    private List<PeEdge> in = new ArrayList<>();
    private List<PeEdge> out = new ArrayList<>();
    //private Node xmlNode;

    public PeNode(String id) {
        this.id = id;
    }

    public PeEdge onlyOneOut() {
        return out.get(0);
    }

    public PeEdge outWithID(String nextPeEdgeID) {
        return out.stream().filter(e -> e.getId().equals(nextPeEdgeID)).findFirst().get();
    }

    public PeEdge outWithOutID(String nextPeEdgeID) {
        return out.stream().filter(e -> !e.getId().equals(nextPeEdgeID)).findFirst().get();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<PeEdge> getIn() {
        return in;
    }

    public void setIn(List<PeEdge> in) {
        this.in = in;
    }

    public List<PeEdge> getOut() {
        return out;
    }

    public void setOut(List<PeEdge> out) {
        this.out = out;
    }

//    public Node getXmlNode() {
//        return xmlNode;
//    }
//
//    public void setXmlNode(Node xmlNode) {
//        this.xmlNode = xmlNode;
//    }
}