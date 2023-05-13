package com.damon.cqrs.event_store;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
public class MysqlEventOffsetTest {

    private MysqlEventOffset offset;

    @Before
    public void before() throws Exception {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3307/cqrs?serverTimezone=UTC&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(20);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getTypeName());

        offset = new MysqlEventOffset(Lists.newArrayList(
                DataSourceMapping.builder().dataSourceName("ds0").tableNumber(2).dataSource(dataSource).build()
        ));
    }
    @Test
    public void after() {
    }

    @Test
    public void testQueryEventOffset() {
        Assert.assertTrue(!offset.queryEventOffset().join().isEmpty());
    }

    @Test
    public void testUpdateEventOffset() {
        List<Map<String, Object>> rows =  offset.queryEventOffset().join();
        rows.forEach(map->{
            Long id = (Long) map.get("id");
            Long offset_id = (Long) map.get("event_offset_id");
            String dataSourceName = (String)map.get("data_source_name");
            boolean status = offset.updateEventOffset(dataSourceName, offset_id + 1, id).join();
            Assert.assertTrue(status);
            boolean status2 = offset.updateEventOffset(dataSourceName, offset_id, id).join();
            Assert.assertTrue(status2);
        });

    }
}