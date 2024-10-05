package com.damon.cqrs.sample.workflow2.workflow;

import com.damon.cqrs.sample.workflow2.workflow.operator.OperatorOfApprovalApply;
import com.damon.cqrs.sample.workflow2.workflow.operator.OperatorOfExclusiveGateway;
import com.damon.cqrs.sample.workflow2.workflow.operator.OperatorOfNotify;
import com.damon.cqrs.sample.workflow2.workflow.operator.OperatorOfUserTask;


public class ProcessEngineTest {

    private static final String xml = """
            <definitions>
                <process id="process_2" name="简单审批例子">
                    <startEvent id="startEvent_1">
                        <outgoing>flow_1</outgoing>
                    </startEvent>
                    <sequenceFlow id="flow_1" sourceRef="startEvent_1" targetRef="approvalApply_1"/>
                    <approvalApply id="approvalApply_1" name="提交申请单">
                        <incoming>flow_1</incoming>
                        <incoming>flow_5</incoming>
                        <outgoing>flow_2</outgoing>
                    </approvalApply>
                    <sequenceFlow id="flow_2" sourceRef="approvalApply_1" targetRef="approval_1"/>
                    <userTask id="approval_1" name="审批">
                        <incoming>flow_2</incoming>
                        <outgoing>flow_3</outgoing>
                    </userTask>
                    <sequenceFlow id="flow_3" sourceRef="approval_1" targetRef="simpleGateway_1"/>
                    <exclusiveGateway id="simpleGateway_1" name="简单是非判断">
                        <incoming>flow_3</incoming>
                        <outgoing>flow_4</outgoing>
                        <outgoing>flow_5</outgoing>
                    </exclusiveGateway>
                    <sequenceFlow id="flow_5" sourceRef="simpleGateway_1" targetRef="approvalApply_1">
                     <![CDATA[
                        price > 200
                     ]]>
                    </sequenceFlow>
                    <sequenceFlow id="flow_4" sourceRef="simpleGateway_1" targetRef="notify_1">
                     <![CDATA[
                        price <= 200
                     ]]>
                    </sequenceFlow>
                    <notify id="notify_1" name="结果邮件通知">
                        <incoming>flow_4</incoming>
                        <outgoing>flow_6</outgoing>
                    </notify>
                    <sequenceFlow id="flow_6" sourceRef="notify_1" targetRef="endEvent_1"/>
                    <endEvent id="endEvent_1">
                        <incoming>flow_6</incoming>
                    </endEvent>
                </process>
            </definitions>
            """;

    public static void main(String[] args) throws Exception {
        //读取文件内容到字符串
        ProcessEngine processEngine = new ProcessEngine(xml);
        processEngine.registNodeProcessor(new OperatorOfUserTask());
        processEngine.registNodeProcessor(new OperatorOfApprovalApply());
        processEngine.registNodeProcessor(new OperatorOfNotify());
        processEngine.registNodeProcessor(new OperatorOfExclusiveGateway());
        processEngine.start();
        Thread.sleep(1000000 * 1);
    }

}