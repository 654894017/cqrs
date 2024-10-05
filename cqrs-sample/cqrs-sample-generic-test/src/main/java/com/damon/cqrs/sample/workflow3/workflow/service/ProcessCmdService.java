package com.damon.cqrs.sample.workflow3.workflow.service;

import cn.hutool.core.util.IdUtil;
import com.damon.cqrs.command.CommandService;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.sample.workflow3.workflow.PeProcess;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessApprovalCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessExclusiveCmd;
import com.damon.cqrs.sample.workflow3.workflow.cmd.ProcessStartCmd;

public class ProcessCmdService extends CommandService<PeProcess> {
    public ProcessCmdService(CqrsConfig config) {
        super(config);
    }

    public boolean startProcess(ProcessStartCmd cmd) {
        return super.process(cmd,
                () -> new PeProcess(cmd.getAggregateId(), cmd.getProcessXml()),
                process -> process.startProcess(new ProcessStartCmd(IdUtil.getSnowflakeNextId(), process.getAggregateId()))
        ).join();
    }

    public boolean approval(ProcessApprovalCmd cmd) {
        return super.process(cmd, process -> process.approval(cmd)).join();
    }

    public boolean exclusive(ProcessExclusiveCmd cmd) {
        return super.process(cmd, process -> process.exclusive(cmd)).join();
    }
}
