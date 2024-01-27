package io.github.xiaoso456.produce;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProduceExactlyOnceDemo {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"172.23.111.68:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");
        // 设置 ack 为-1
        properties.put(ProducerConfig.ACKS_CONFIG,"-1");
        // 开启幂等性
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        // 设置事务id
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "test-transaction-01");
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        // 初始化事务
        kafkaProducer.initTransactions();
        // 开启事务
        kafkaProducer.beginTransaction();

        kafkaProducer.send(new ProducerRecord<>("test-topic", "key", "value"), new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (e != null) {
                    e.printStackTrace();
                    System.out.println("发送消息失败");
                    // 发送失败，取消事务
                    kafkaProducer.abortTransaction();
                    return;
                }
                // 发送成功，提交事务
                kafkaProducer.commitTransaction();
                System.out.println("发送消息成功，分区" +recordMetadata.partition() + "，偏移量" + recordMetadata.offset());
            }
        });

        kafkaProducer.close();

    }
}
