package com.damon.cqrs.sample.workflow.domain;

import java.util.Map;

public interface IWorkflowCommandHandler {

    Map<String, String> getNodeAttributes(String id);

    Map<String, Object> create();

    Map<String,Object> start();

    void stop();

    void processing();




}
