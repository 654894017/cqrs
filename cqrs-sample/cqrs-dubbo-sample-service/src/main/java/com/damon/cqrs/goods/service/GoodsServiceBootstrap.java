package com.damon.cqrs.goods.service;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.damon.cqrs.DefaultAggregateCache;
import com.damon.cqrs.DefaultAggregateSnapshootService;
import com.damon.cqrs.EventCommittingService;
import com.damon.cqrs.IAggregateCache;
import com.damon.cqrs.IAggregateSnapshootService;
import com.damon.cqrs.IEventStore;
import com.damon.cqrs.store.MysqlEventStore;
import com.zaxxer.hikari.HikariDataSource;

@EnableAutoConfiguration
public class GoodsServiceBootstrap {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/enode?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    @Bean
    public EventCommittingService eventCommittingService(@Autowired DataSource dataSource) {
        IEventStore store = new MysqlEventStore(new JdbcTemplate(dataSource));
        IAggregateSnapshootService snapshootService = new DefaultAggregateSnapshootService(50, 5);
        IAggregateCache aggregateCache = new DefaultAggregateCache(1024 * 1024, 30);
        return new EventCommittingService(store, snapshootService, aggregateCache, 1024, 1024);
    }

    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceBootstrap.class, args);
    }

}