package com.damon.cqrs.goods.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GoodsServiceBootstrap {

    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceBootstrap.class, args);
    }

}