package io.github.xiaoso456.produce;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProduceSyncDemo {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"my-kafka.default.svc.cluster.local:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        // kafka 发送后返回一个future,调用get方法即可变为同步发送
        Future<RecordMetadata> metadataFuture = kafkaProducer.send(new ProducerRecord<>("test-topic", "key", "value"));
        try {
            RecordMetadata recordMetadata = metadataFuture.get();
            System.out.println("发送消息成功，分区" +recordMetadata.partition() + "，偏移量" + recordMetadata.offset());

        } catch (ExecutionException | InterruptedException e) {
            System.out.println("发送失败");
            e.printStackTrace();
        }

        kafkaProducer.close();

    }
}
