package com.damon.cqrs.domain;


import lombok.Data;

import java.io.Serializable;

@Data
public abstract class ValueObject implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -489158980325174474L;

}
