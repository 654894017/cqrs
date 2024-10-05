package com.damon.cqrs.sample.red_packet;

import com.damon.cqrs.cache.DefaultAggregateCaffeineCache;
import com.damon.cqrs.cache.IAggregateCache;
import com.damon.cqrs.config.AggregateSlotLock;
import com.damon.cqrs.config.CqrsConfig;
import com.damon.cqrs.event.EventCommittingService;
import com.damon.cqrs.event_store.DataSourceMapping;
import com.damon.cqrs.event_store.DefaultEventShardingRouting;
import com.damon.cqrs.event_store.MysqlEventStore;
import com.damon.cqrs.recovery.AggregateRecoveryService;
import com.damon.cqrs.sample.red_packet.domain.service.RedPacketCommandService;
import com.damon.cqrs.snapshot.DefaultAggregateSnapshootService;
import com.damon.cqrs.snapshot.IAggregateSnapshootService;
import com.damon.cqrs.store.IEventStore;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class RedPacketConfig {
    @Bean
    public HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3307/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(80);
        dataSource.setMinimumIdle(80);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public CqrsConfig config(JdbcTemplate jdbcTemplate) {
        List<DataSourceMapping> list = Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").dataSource(dataSource()).tableNumber(4).build()
        );
        DefaultEventShardingRouting route = new DefaultEventShardingRouting();
        IEventStore store = new MysqlEventStore(list, 32, route);
        IAggregateSnapshootService aggregateSnapshootService = new DefaultAggregateSnapshootService(8, 6);
        IAggregateCache aggregateCache = new DefaultAggregateCaffeineCache(1024 * 1024, 60);
        AggregateSlotLock aggregateSlotLock = new AggregateSlotLock(4096);
        AggregateRecoveryService aggregateRecoveryService = new AggregateRecoveryService(store, aggregateCache, aggregateSlotLock);
        EventCommittingService eventCommittingService = new EventCommittingService(store, 8, 1024 * 4, 32, aggregateRecoveryService);
        CqrsConfig cqrsConfig = CqrsConfig.builder().
                eventStore(store).aggregateSnapshootService(aggregateSnapshootService).aggregateCache(aggregateCache).
                aggregateSlotLock(aggregateSlotLock).
                eventCommittingService(eventCommittingService).build();
        return cqrsConfig;
    }

    @Bean
    public RedPacketCommandService redPacketCommandService(CqrsConfig cqrsConfig) {
        return new RedPacketCommandService(cqrsConfig);
    }


}
