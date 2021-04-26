package com.damon.cqrs.goods.api;

import java.io.Serializable;

import lombok.Data;

@Data
public class GoodsDO implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 2095748087072119778L;

    private long id;

    private int number;

    private String name;

}
