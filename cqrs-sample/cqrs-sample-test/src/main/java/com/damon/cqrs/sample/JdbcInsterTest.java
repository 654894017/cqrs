package com.damon.cqrs.sample;

import java.sql.*;

/**
 * MySql 插入(insert)性能测试
 * Oracle 插入(insert)性能测试
 * MySql建表语句：
 * CREATE  TABLE `dev`.`test_insert` (
 * `id` INT NOT NULL ,
 * `uname` VARCHAR(10) NULL ,
 * PRIMARY KEY (`id`) )
 * ENGINE = InnoDB;
 */
public class JdbcInsterTest {

    static int count = 100000;//总次数

    //一定要写rewriteBatchedStatements参数，Mysql批量插入才性能才理想
    static String mySqlUrl = "jdbc:mysql://127.0.0.1:3306/cqrs?rewriteBatchedStatements=true";
    static String mySqlUserName = "root";
    static String mySqlPassword = "root";

    static String oracleurl = "jdbc:oracle:thin:@192.168.10.139:1521:orcl";
    static String oracleuserName = "scott";
    static String oraclepassword = "tiger";

    static String sql = "insert into test_insert(id,uname) values(?,?)";

    //每执行几次提交一次
    static int[] commitPoint = {count, 10000, 1000, 100, 10, 1};

    public static void main(String[] args) {
        for (int point : commitPoint) {
            test_mysql(point);
        }
        for (int point : commitPoint) {
            test_mysql_batch(point);
        }
//    	for(int point:commitPoint){
//            test_oracle(point);  
//    	}
//    	for(int point:commitPoint){
//            test_oracle_batch(point);  
//    	}
    }

    /**
     * 创建连接
     *
     * @return
     */
    public static Connection getConn(String flag) {
        long a = System.currentTimeMillis();
        try {
            if ("mysql".equals(flag)) {
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(mySqlUrl, mySqlUserName, mySqlPassword);
                conn.setAutoCommit(false);
                return conn;
            } else if ("oracle".equals(flag)) {
                Class.forName("oracle.jdbc.OracleDriver");
                Connection conn = DriverManager.getConnection(oracleurl, oracleuserName, oraclepassword);
                conn.setAutoCommit(false);
                return conn;
            } else {
                System.out.println();
                throw new RuntimeException("flag参数不正确,flag=" + flag);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            long b = System.currentTimeMillis();
            System.out.println("创建连接用时" + (b - a) + " ms");
        }
        return null;
    }

    /**
     * 关闭连接
     *
     * @return
     */
    public static void close(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除旧数据
     *
     * @return
     */
    public static void clear(Connection conn) {
        try {
            Statement st = conn.createStatement();
            boolean bl = st.execute("delete FROM test_insert");
            conn.commit();
            st.close();
            System.out.println("执行清理操作：" + (bl == false ? "成功" : "失败"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印信息
     *
     * @return
     */
    public static void print(String key, long startTime, long endTime, int point) {
        System.out.println("每执行" + point + "次sql提交一次事务");
        System.out.println(key + "，用时" + (endTime - startTime) + " ms,平均每秒执行" + (count * 1000 / (endTime - startTime)) + "条");
        System.out.println("----------------------------------");
    }

    /**
     * mysql非批量插入10万条记录
     */
    public static void test_mysql(int point) {
        Connection conn = getConn("mysql");
        clear(conn);
        try {
            PreparedStatement prest = conn.prepareStatement(sql);
            long a = System.currentTimeMillis();
            for (int x = 1; x <= count; x++) {
                prest.setInt(1, x);
                prest.setString(2, "张三");
                prest.execute();
                if (x % point == 0) {
                    conn.commit();
                }
            }
            long b = System.currentTimeMillis();
            print("MySql非批量插入10万条记录", a, b, point);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(conn);
        }
    }

    /**
     * mysql批量插入10万条记录
     */
    public static void test_mysql_batch(int point) {
        Connection conn = getConn("mysql");
        clear(conn);
        try {
            PreparedStatement prest = conn.prepareStatement(sql);
            long a = System.currentTimeMillis();
            for (int x = 1; x <= count; x++) {
                prest.setInt(1, x);
                prest.setString(2, "张三");
                prest.addBatch();
                if (x % point == 0) {
                    prest.executeBatch();
                    conn.commit();
                }
            }
            long b = System.currentTimeMillis();
            print("MySql批量插入10万条记录", a, b, point);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(conn);
        }
    }

    /**
     * oracle非批量插入10万条记录
     */
    public static void test_oracle(int point) {
        Connection conn = getConn("oracle");
        clear(conn);
        try {
            PreparedStatement prest = conn.prepareStatement(sql);
            long a = System.currentTimeMillis();
            for (int x = 1; x <= count; x++) {
                prest.setInt(1, x);
                prest.setString(2, "张三");
                prest.execute();
                if (x % point == 0) {
                    conn.commit();
                }
            }
            long b = System.currentTimeMillis();
            print("Oracle非批量插入10万记录", a, b, point);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(conn);
        }
    }

    /**
     * oracle批量插入10万条记录
     */
    public static void test_oracle_batch(int point) {
        Connection conn = getConn("oracle");
        clear(conn);
        try {
            PreparedStatement prest = conn.prepareStatement(sql);
            long a = System.currentTimeMillis();
            for (int x = 1; x <= count; x++) {
                prest.setInt(1, x);
                prest.setString(2, "张三");
                prest.addBatch();
                if (x % point == 0) {
                    prest.executeBatch();
                    conn.commit();
                }
            }
            long b = System.currentTimeMillis();
            print("Oracle批量插入10万记录", a, b, point);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(conn);
        }
    }
}  