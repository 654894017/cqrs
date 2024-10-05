package com.damon.cqrs.sample.workflow3.workflow;

import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessApprovalCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessExclusiveCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessStartCmd;
import com.damon.cqrs.sample.workflow3.workflow.config.WorkflowConfig;
import com.damon.cqrs.sample.workflow3.workflow.operator.OperatorOfApprovalApply;
import com.damon.cqrs.sample.workflow3.workflow.operator.OperatorOfExclusiveGateway;
import com.damon.cqrs.sample.workflow3.workflow.operator.OperatorOfNotify;
import com.damon.cqrs.sample.workflow3.workflow.operator.OperatorOfUserTask;
import com.damon.cqrs.sample.workflow3.workflow.service.ProcessCmdService;


public class ProcessEngineTest {

    private static final String xml = """
            <definitions>
                <process id="111" name="简单审批例子">
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
        ProcessEngine.registNodeProcessor(new OperatorOfUserTask());
        ProcessEngine.registNodeProcessor(new OperatorOfApprovalApply());
        ProcessEngine.registNodeProcessor(new OperatorOfNotify());
        ProcessEngine.registNodeProcessor(new OperatorOfExclusiveGateway());

        CqrsConfig config = WorkflowConfig.init();
        ProcessCmdService processCmdService = new ProcessCmdService(config);
        ProcessStartCmd startCmd = new ProcessStartCmd(1L, 1l);
        startCmd.setProcessXml(xml);
        processCmdService.startProcess(startCmd);

        ProcessApprovalCmd cmd = new ProcessApprovalCmd(2L, 1L);
        cmd.setNodeId("approval_1");
        processCmdService.approval(cmd);

        ProcessExclusiveCmd cmd2 = new ProcessExclusiveCmd(3L, 1L);
        cmd2.setNodeId("simpleGateway_1");
        processCmdService.exclusive(cmd2);

        ProcessApprovalCmd cmd3 = new ProcessApprovalCmd(4L, 1L);
        cmd3.setNodeId("approvalApply_1");
        processCmdService.approval(cmd3);

        ProcessApprovalCmd cmd4 = new ProcessApprovalCmd(5L, 1L);
        cmd4.setNodeId("approval_1");
        processCmdService.approval(cmd4);

        ProcessExclusiveCmd cmd5 = new ProcessExclusiveCmd(6L, 1L);
        cmd5.setNodeId("simpleGateway_1");
        processCmdService.exclusive(cmd5);

        ProcessApprovalCmd cmd7 = new ProcessApprovalCmd(7L, 1L);
        cmd7.setNodeId("approvalApply_1");
        processCmdService.approval(cmd7);

        ProcessApprovalCmd cmd8 = new ProcessApprovalCmd(8L, 1L);
        cmd8.setNodeId("approval_1");
        processCmdService.approval(cmd8);

        ProcessExclusiveCmd cmd9 = new ProcessExclusiveCmd(9L, 1L);
        cmd9.setNodeId("simpleGateway_1");
        processCmdService.exclusive(cmd9);

        ProcessApprovalCmd cmd10 = new ProcessApprovalCmd(10L, 1L);
        cmd10.setNodeId("notify_1");
        processCmdService.approval(cmd10);

        ProcessApprovalCmd cmd11 = new ProcessApprovalCmd(11L, 1L);
        cmd11.setNodeId("notify_1");
        processCmdService.approval(cmd11);

        Thread.sleep(1000000 * 1);
    }


}