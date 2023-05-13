import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * kafka消费者
 * <p>
 * https://blog.csdn.net/chinawangfei/article/details/115468977
 */
public class KafkaConsumerTest {
    @Test
    public void test01() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "10.230.5.244:9092,10.230.4.87:9092,10.230.5.152:9092");
        props.put("group.id", "test304");
        //org.apache.kafka.clients.consumer.RangeAssignor asdf3;
        //props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", 8);
        props.put("max.poll.interval.ms", "30000");
        props.put("session.timeout.ms", "30000");
        // props.put("auto.commit.interval.ms", "0");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        /**
         * 如果存在已经提交的offest时,不管设置为earliest 或者latest 都会从已经提交的offest处开始消费
         * 如果不存在已经提交的offest时,earliest 表示从头开始消费,latest 表示从最新的数据消费,也就是新产生的数据.
         * none topic各分区都存在已提交的offset时，从提交的offest处开始消费；只要有一个分区不存在已提交的offset，则抛出异常
         */
        /**
         * none
         * 如果没有为消费者找到先前的offset的值,即没有自动维护偏移量,也没有手动维护偏移量,则抛出异常
         *
         * earliest
         * 在各分区下有提交的offset时：从offset处开始消费在各分区下无提交的offset时：从头开始消费
         *
         * latest
         * 在各分区下有提交的offset时：从offset处开始消费在各分区下无提交的offset时：从最新的数据开始消费
         *
         */
        props.put("auto.offset.reset", "earliest");
        // KafkaConsumer类不是线程安全的
        org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test20200519")); // 订阅topic
        try {
            for (; ; ) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.partition() + ":" + record.key() + ":" + record.value());
                    //Thread.sleep(1000);
                }
                consumer.commitSync();
            }
        } finally {
            consumer.close();
        }
    }

}