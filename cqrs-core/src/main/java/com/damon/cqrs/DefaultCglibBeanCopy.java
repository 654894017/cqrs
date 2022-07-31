package com.damon.cqrs;

import cn.hutool.extra.cglib.CglibUtil;

public class DefaultCglibBeanCopy implements IBeanCopy {
    @Override
    public void copy(Object source, Object target) {
        CglibUtil.copy(source, target);
    }
}
