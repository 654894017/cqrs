package com.damon.cqrs.rocketmq;

import java.io.Serializable;

/**
 * 响应统一封装类
 * 注意：必须实现 Serializable 接口，因为默认的编码器：ProtocolCodeBasedEncoder extends MessageToByteEncoder<Serializable>，
 * 只对 Serializable 实现类进行编码
 */
public class MyResponse implements Serializable {
    private static final long serialVersionUID = -6215194863976521002L;
    private String resp = "default resp from server";

    public String getResp() {
        return resp;
    }

    public void setResp(String resp) {
        this.resp = resp;
    }

    @Override
    public String toString() {
        return resp;
    }
}