package com.damon.cqrs.goods.api;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoodsDTO implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 2095748087072119778L;

    private long id;

    private int number;

    private String name;

}
