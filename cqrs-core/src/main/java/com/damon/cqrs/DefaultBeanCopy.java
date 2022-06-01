package com.damon.cqrs;

import com.damon.cqrs.utils.BeanCopierUtils;

public class DefaultBeanCopy implements IBeanCopy {
    @Override
    public void copy(Object source, Object target) {
        BeanCopierUtils.copy(source, target);
    }
}
