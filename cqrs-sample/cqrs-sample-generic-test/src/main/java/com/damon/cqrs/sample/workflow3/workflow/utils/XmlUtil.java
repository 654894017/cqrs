package com.damon.cqrs.sample.workflow3.workflow.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlUtil {
    public static Node strToNode(String xmlStr) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        return doc.getDocumentElement();
    }

    public static Node childByName(Node parent, String nodeName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (nodeName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    public static String childTextByName(Node parent, String childName) {
        Node childNode = childByName(parent, childName);
        if (childNode != null) {
            return childNode.getTextContent();
        }
        return null;
    }

    public static String attributeValue(Node node, String attributeName) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            return element.getAttribute(attributeName);
        }
        return null;
    }

    public static String text(Node node) {
        return node.getTextContent();
    }

    public static List<Node> childsByName(Node parent, String childName) {
        List<Node> childNodes = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (childName.equals(child.getNodeName())) {
                childNodes.add(child);
            }
        }
        return childNodes;
    }
}
