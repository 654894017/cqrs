package com.damon.cqrs.sample.workflow2.workflow;

import com.damon.cqrs.sample.workflow2.workflow.operator.IOperator;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ProcessEngine {
    //任务数据暂存
    public final BlockingQueue<PeNode> arrayBlockingQueue = new LinkedBlockingQueue();
    private String xmlStr;
    //存储算子
    private Map<String, IOperator> type2Operator = new ConcurrentHashMap<>();
    private PeProcess peProcess = null;
    private PeContext peContext = null;
    //任务调度线程
    public final Thread dispatchThread = new Thread(() -> {
        while (true) {
            try {
                PeNode peNode = arrayBlockingQueue.take();
                type2Operator.get(peNode.getType()).doTask(this, peNode, peContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public ProcessEngine(String xmlStr) {
        this.xmlStr = xmlStr;
    }

    //算子注册到引擎中，便于引擎调用之
    public void registNodeProcessor(IOperator operator) {
        type2Operator.put(operator.getType(), operator);
    }

    public void start() throws Exception {
        peProcess = new XmlProcessBuilder(xmlStr).build();
        peContext = new PeContext();
        dispatchThread.setDaemon(true);
        dispatchThread.start();
        executeNode(peProcess.getStart().onlyOneOut().getTo());
    }

    private void executeNode(PeNode peNode) {
        if (!peNode.getType().equals("endEvent"))
            arrayBlockingQueue.add(peNode);
        else
            System.out.println("process finished!");
    }

    public void nodeFinished(PeEdge nextPeEdgeID) {
        executeNode(nextPeEdgeID.getTo());
    }
}