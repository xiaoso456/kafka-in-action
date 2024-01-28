package io.github.xiaoso456.produce;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProduceDemo {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"172.31.74.189:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        // kafka 默认发送为异步发送，ack方式为1
        kafkaProducer.send(new ProducerRecord<>("test-topic", "key", "value"), new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (e != null) {
                    e.printStackTrace();
                    System.out.println("发送消息失败");
                    return;
                }
                System.out.println("发送消息成功，分区" +recordMetadata.partition() + "，偏移量" + recordMetadata.offset());
            }
        });
        kafkaProducer.close();

    }
}
