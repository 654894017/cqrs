package com.damon.cqrs.sample.workflow.domain.node;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;

public class SequenceFlow implements IElement {

    private IElement sourceRef;

    private IElement targetRef;

    @Override
    public boolean matching(Node node) {
        return false;
    }

    @Override
    public void parse(Document doc, XPath xpath, String id) throws XPathExpressionException {

    }

    @Override
    public List<IElement> next() {
        return null;
    }
}
