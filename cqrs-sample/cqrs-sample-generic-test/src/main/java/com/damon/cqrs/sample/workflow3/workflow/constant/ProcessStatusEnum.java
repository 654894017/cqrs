package com.damon.cqrs.sample.workflow3.workflow.constant;

public enum ProcessStatusEnum {

    START(0, "开启"), GOING(1, "进行中"), FINISHED(2, "已完成"), STOPPED(3, "已终止");
    private Integer status;
    private String name;

    ProcessStatusEnum(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
