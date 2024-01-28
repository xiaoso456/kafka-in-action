package io.github.xiaoso456.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;

public class ConsumerOffsetTimeSeek {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.31.74.189:9092");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-topic-group-2");


        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        Set<String> topics = Collections.singleton("test-topic");
        kafkaConsumer.subscribe(topics);
        // 从指定位置开始消费
        Set<TopicPartition> assignment = kafkaConsumer.assignment();
        // 等待消费者获取到消费分区信息
        while (assignment.size() == 0) {
            kafkaConsumer.poll(Duration.ofSeconds(1));
            assignment = kafkaConsumer.assignment();
        }

        // 保存每个 partition 希望指定的位移时间，这里希望 offset 设置到一天前
        Map<TopicPartition, Long> topicPartitionTimeMap = new HashMap<>();
        for (TopicPartition topicPartition : assignment) {
            topicPartitionTimeMap.put(topicPartition, System.currentTimeMillis() - 1 * 24 * 3600 * 100);
        }
        // 调用 kafka api 根据希望的 timestamp 匹配 offset
        Map<TopicPartition, OffsetAndTimestamp> topicPartitionOffsetAndTimestampMap = kafkaConsumer.offsetsForTimes(topicPartitionTimeMap);

        for (TopicPartition topicPartition : assignment) {
            // 设置 offset 
            OffsetAndTimestamp offsetAndTimestamp = topicPartitionOffsetAndTimestampMap.get(topicPartition);
            kafkaConsumer.seek(topicPartition, offsetAndTimestamp.offset());
        }
        while (true) {
            // kafka 拉取数据, 每批次间隔 1 秒
            ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> record : consumerRecords) {
                System.out.println(record);
            }
        }
    }
}