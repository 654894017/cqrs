import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

import java.util.Properties;

/**
 * kafka生产者
 * <p>
 * https://blog.csdn.net/chinawangfei/article/details/115468977
 */
public class KafkaProducerTest {
    @Test
    public void test01() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "10.230.5.244:9092,10.230.4.87:9092,10.230.5.152:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        org.apache.kafka.clients.producer.KafkaProducer<String, String> kafkaProducer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(properties);
        for (int i = 1; i <= 3; i++) {
            //参数1：topic名, 参数2：消息文本； ProducerRecord多个重载的构造方法
            kafkaProducer.send(new ProducerRecord<String, String>("test20200519", i + "", "message" + i));
            System.out.println("message" + i);
        }
        kafkaProducer.flush();
        kafkaProducer.close();
    }
}