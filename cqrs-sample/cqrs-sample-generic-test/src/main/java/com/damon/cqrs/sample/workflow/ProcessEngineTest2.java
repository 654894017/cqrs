package com.damon.cqrs.sample.workflow;

import com.damon.cqrs.sample.workflow.operator.OperatorOfApproval;
import com.damon.cqrs.sample.workflow.operator.OperatorOfApprovalApply;
import com.damon.cqrs.sample.workflow.operator.OperatorOfNotify;
import com.damon.cqrs.sample.workflow.operator.OperatorOfSimpleGateway;


public class ProcessEngineTest2 {

    private static final String xml = """
            <definitions>
                <process id="process_2" name="简单审批例子">
                    <startEvent id="startEvent_1" formKey = "abc">
                        <incoming>flow_5</incoming>
                        <outgoing>flow_2</outgoing>
                    </startEvent>
                    <sequenceFlow id="flow_2" sourceRef="startEvent_1" targetRef="approvalApply_1"/>
                    <approval id="approvalApply_1" name="审批">
                        <incoming>flow_2</incoming>
                        <outgoing>flow_3</outgoing>
                    </approval>
                    <sequenceFlow id="flow_3" sourceRef="approvalApply_1" targetRef="simpleGateway_1"/>
                    <simpleGateway id="simpleGateway_1" name="简单是非判断">
                        <trueOutGoing>flow_4</trueOutGoing>
                        <expr>approvalResult</expr>
                        <incoming>flow_3</incoming>
                        <outgoing>flow_4</outgoing>
                        <outgoing>flow_5</outgoing>
                    </simpleGateway>
                    <sequenceFlow id="flow_5" sourceRef="simpleGateway_1" targetRef="approvalApply_1"/>
                    <sequenceFlow id="flow_4" sourceRef="simpleGateway_1" targetRef="notify_1">aaaaaaa</sequenceFlow>
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
        processEngine.registNodeProcessor(new OperatorOfApproval());
        processEngine.registNodeProcessor(new OperatorOfApprovalApply());
        processEngine.registNodeProcessor(new OperatorOfNotify());
        processEngine.registNodeProcessor(new OperatorOfSimpleGateway());
        processEngine.start();
        Thread.sleep(1000000 * 1);
    }

}