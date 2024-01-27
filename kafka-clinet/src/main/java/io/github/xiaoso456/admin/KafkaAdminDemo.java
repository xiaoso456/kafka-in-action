package io.github.xiaoso456.admin;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class KafkaAdminDemo {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"172.23.111.68:9092");
        // 创建 AdminClient
        try (AdminClient adminClient = AdminClient.create(properties)) {
            // 列出所有 topic
            adminClient.listTopics().names().get().forEach(System.out::println);

            // 创建一个名为 test 的 topic, 2 个分区, 2 个副本
            NewTopic newTopic = new NewTopic("test", 2, (short) 2);
            adminClient.createTopics(Collections.singleton(newTopic));

            // 查看名为 test topic 的分区和副本数
            Map<String, TopicDescription> topicPartitionInfoMap = adminClient.describeTopics(Collections.singletonList("test")).allTopicNames().get();
            topicPartitionInfoMap.forEach((k, v) -> {
                System.out.println("topic: " + k);
                for (TopicPartitionInfo partitionInfo : v.partitions()) {
                    System.out.println("partition: " + partitionInfo.partition() + " leader: " + partitionInfo.leader().toString() + " replicas: " + Arrays.toString(partitionInfo.replicas().toArray()) + " isr: " + Arrays.toString(partitionInfo.isr().toArray()));
                }
            });

            // 删除名为 test 的 topic
            adminClient.deleteTopics(Collections.singletonList("test"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
