package com.damon.cqrs.sample.workflow3.workflow;

import cn.hutool.core.util.ObjectUtil;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessApprovalCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessExclusiveCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessStartCmd;
import com.damon.cqrs.sample.workflow3.workflow.constant.ProcessStatusEnum;
import com.damon.cqrs.sample.workflow3.workflow.event.ProcessApprovedEvent;
import com.damon.cqrs.sample.workflow3.workflow.event.ProcessCreatedEvent;
import com.damon.cqrs.sample.workflow3.workflow.event.ProcessExclusivedEvent;
import com.damon.cqrs.sample.workflow3.workflow.event.ProcessStartedEvent;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class PeProcess extends AggregateRoot {
    private transient PeNode start;
    private Long aggregateId;
    private PeContext context;
    /**
     * 0 表示未开始  1 进行中   2 已完成  3 已终止
     */
    private ProcessStatusEnum status;
    private Set<String> goingNodeIds;


    public PeProcess(Long aggregateId, String xml) {
        super(aggregateId);
        applyNewEvent(new ProcessCreatedEvent(aggregateId, xml));
    }

    /**
     * 获取节点id对应的流程节点（递归）
     *
     * @param nodeId
     * @return
     */
    public PeNode getNode(String nodeId) {
        PeNode node = get(nodeId, start);
        if (node == null) {
            throw new RuntimeException("无效的节点id:" + nodeId);
        }
        return node;
    }

    private PeNode get(String nodeId, PeNode node) {
        if (node.getId().equals(nodeId)) {
            return node;
        }
        for (PeEdge edge : node.getOut()) {
            PeNode outNode = edge.getTo();
            if (outNode.getId().equals(nodeId)) {
                return outNode;
            } else {
                return get(nodeId, outNode);
            }
        }
        return null;
    }

    public boolean startProcess(ProcessStartCmd cmd) {
        PeNode node = start.onlyOneOut().getTo();
        ProcessEngine.doTask(node, this);
        PeNode next = node.onlyOneOut().getTo();
        ProcessStartedEvent event = new ProcessStartedEvent();
        event.setCurNodeId(start.getId());
        event.setNextNodeId(next.getId());
        event.setCurNodeType(next.getType());
        event.setContext(context);
        applyNewEvent(event);
        return Boolean.TRUE;
    }


    public Long getId() {
        return aggregateId;
    }

    public void setId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public boolean exclusive(ProcessExclusiveCmd cmd) {
        PeNode node = getNode(cmd.getNodeId());
        ProcessEngine.doTask(node, this);
        return Boolean.TRUE;
    }

    public boolean route(PeEdge peEdge) {
        ProcessExclusivedEvent event = new ProcessExclusivedEvent();
        PeNode from = peEdge.getFrom();
        PeNode nextNode = peEdge.getTo();
        event.setNextNodeId(nextNode.getId());
        event.setContext(context);
        event.setNextNodeType(nextNode.getType());
        event.setCurNodeId(from.getId());
        event.setCurNodeType(from.getType());
        applyNewEvent(event);
        return Boolean.TRUE;
    }


    public boolean approval(ProcessApprovalCmd cmd) {
        String nodeId = cmd.getNodeId();
        PeNode node = getNode(nodeId);
        if (isNodeExecuted(nodeId)) {
            throw new RuntimeException("该节点已执行无法重复执行，节点id:" + nodeId);
        }
        ProcessEngine.doTask(node, this);
        ProcessApprovedEvent event = new ProcessApprovedEvent();
        event.setCurNodeId(nodeId);
        event.setCurNodeType(node.getType());
        event.setContext(context);
        PeNode next = node.onlyOneOut().getTo();
        event.setNextNodeId(next.getId());
        event.setNextNodeType(next.getType());
        applyNewEvent(event);
        return Boolean.TRUE;
    }

    public Boolean isFinished() {
        return ObjectUtil.equals(ProcessStatusEnum.FINISHED, status);
    }

    public Boolean isNodeExecuted(String nodeId) {
        return !goingNodeIds.contains(nodeId);
    }

    public Boolean isStopped() {
        return ObjectUtil.equals(ProcessStatusEnum.STOPPED, status);
    }

    public Boolean isEndEvent(String nodeType) {
        return ObjectUtil.equals("endEvent", nodeType);
    }

    private void apply(ProcessApprovedEvent event) {
        if (isEndEvent(event.getNextNodeType())) {
            this.status = ProcessStatusEnum.FINISHED;
            this.goingNodeIds.clear();
        } else {
            this.goingNodeIds.remove(event.getCurNodeId());
            this.goingNodeIds.add(event.getNextNodeId());
            this.context = event.getContext();
        }
    }

    private void apply(ProcessExclusivedEvent event) {
        goingNodeIds.remove(event.getCurNodeId());
        goingNodeIds.add(event.getNextNodeId());
        this.context = event.getContext();
    }

    private void apply(ProcessStartedEvent event) {
        goingNodeIds.remove(event.getCurNodeId());
        goingNodeIds.add(event.getNextNodeId());
        this.context = event.getContext();
    }

    private void apply(ProcessCreatedEvent event) {
        this.start = event.getStart();
        this.aggregateId = event.getAggregateId();
        this.goingNodeIds = new HashSet<>();
        XmlProcessBuilder builder = new XmlProcessBuilder(event.getXml());
        this.start = builder.build();
        this.context = new PeContext();
        this.status = ProcessStatusEnum.START;
    }

}


 
