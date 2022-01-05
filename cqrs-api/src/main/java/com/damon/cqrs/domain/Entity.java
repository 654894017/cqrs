package com.damon.cqrs.domain;

import java.io.Serializable;

/**
 * 子类需要实现无参构造方法，不然bean复制时报错
 *
 * @author xianping_lu
 */
public abstract class Entity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 8802825260891973264L;

    private long id;

    public Entity() {
    }

    public Entity(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}