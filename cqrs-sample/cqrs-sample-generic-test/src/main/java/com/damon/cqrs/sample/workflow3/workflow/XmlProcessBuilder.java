package com.damon.cqrs.sample.workflow3.workflow;

import com.damon.cqrs.sample.workflow3.workflow.utils.XmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlProcessBuilder {
    private final Map<String, PeNode> id2PeNode = new HashMap<>();
    private final Map<String, PeEdge> id2PeEdge = new HashMap<>();
    private String xmlStr;

    public XmlProcessBuilder(String xmlStr) {
        this.xmlStr = xmlStr;
    }

    public PeNode build() {
        //strToNode : 把一段xml转换为org.w3c.dom.Node
        Node definations = null;
        try {
            definations = XmlUtil.strToNode(xmlStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //childByName : 找到definations子节点中nodeName为process的那个Node
        Node process = XmlUtil.childByName(definations, "process");
        NodeList childNodes = process.getChildNodes();

        for (int j = 0; j < childNodes.getLength(); j++) {
            Node node = childNodes.item(j);
            //#text node should be skip
            if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) continue;

            if ("sequenceFlow".equals(node.getNodeName()))
                buildPeEdge(node);
            else
                buildPeNode(node);
        }
        Map.Entry<String, PeNode> startEventEntry = id2PeNode.entrySet().stream().filter(entry -> "startEvent".equals(entry.getValue().getType())).findFirst().get();
        return startEventEntry.getValue();
    }

    private void buildPeEdge(Node node) {
        String expandInfo = XmlUtil.text(node);
        //attributeValue : 找到node节点上属性为id的值
        String nodeId = XmlUtil.attributeValue(node, "id");
        PeEdge peEdge = id2PeEdge.get(nodeId);
        if (peEdge != null) {
            peEdge.setExpandInfo(expandInfo);
        } else {
            id2PeEdge.put(nodeId, new PeEdge(nodeId, expandInfo));
        }
        peEdge.setFrom(id2PeNode.computeIfAbsent(XmlUtil.attributeValue(node, "sourceRef"), id -> new PeNode(id)));
        peEdge.setTo(id2PeNode.computeIfAbsent(XmlUtil.attributeValue(node, "targetRef"), id -> new PeNode(id)));
    }

    public Map<String, PeNode> getId2PeNode() {
        return id2PeNode;
    }

    public Map<String, PeEdge> getId2PeEdge() {
        return id2PeEdge;
    }

    private void buildPeNode(Node node) {
        PeNode peNode = id2PeNode.computeIfAbsent(XmlUtil.attributeValue(node, "id"), id -> new PeNode(id));
        peNode.setType(node.getNodeName());
        //peNode.setXmlNode(node);

        List<Node> inPeEdgeNodes = XmlUtil.childsByName(node, "incoming");
        inPeEdgeNodes.stream().forEach(n -> {
            peNode.getIn().add(id2PeEdge.computeIfAbsent(XmlUtil.text(n), id -> new PeEdge(id)));
        });

        List<Node> outPeEdgeNodes = XmlUtil.childsByName(node, "outgoing");
        outPeEdgeNodes.stream().forEach(n -> {
            peNode.getOut().add(id2PeEdge.computeIfAbsent(XmlUtil.text(n), id -> new PeEdge(id)));
        });
    }
}
