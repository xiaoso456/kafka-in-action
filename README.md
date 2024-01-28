## 简介

## 快速启动

### 单机

```sh
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
bin/kafka-server-start.sh config/kraft/server.properties
```

## Kafka 常用概念

ISR（in-sync replica set）：和 Leader 保持同步的 Follower+Leader 集合，例如 `leader:0,isr:0,1,2`，如果超过 `replica.lag.time.max.ms`
设置的时间（30s）未和 Leader 通信，那么会被从 ISR 中去除

### producer client

kafka 生产者发送一个消息通常会经过拦截器（Interceptor）、序列化器（Serializer）、分区器（Partitioner）

```mermaid
graph LR

producer-->interceptor-->serializer-->partitioner
```

Kafka producer 一些常用参数如下:

+ batch.size

  数据积累到 batch.size 后，sender 发送数据。默认值为 16K

+ linger.ms:

  消息最大发送延迟，默认值为 0ms。如果设置 5ms，当消息大小没有达到 16KB（batch.size）时，如果 5ms 内没有新的消息发送，也会触发消息发送

  增大 linger.ms 可以提供生产者吞吐量，但不建议调整过大，会导致延迟过高，如果存在吞吐量瓶颈，建议调整至 5~100ms

+ compression.type

  消息压缩类型，默认为 `none`

  以下是几种常见的 `compression.type` 选项：

    1. `none`：表示不使用任何压缩。消息将以原始未压缩的形式发送给 Kafka 集群。
    2. `gzip`：Gzip 提供了较高的压缩比，但会增加一些计算开销。
    3. `snappy`：Snappy 提供了较低的压缩比，但压缩和解压缩速度较快，适用于高吞吐量的场景。
    4. `lz4`：LZ4 提供了较高的压缩和解压缩速度，并具有较低的 CPU 开销。
    5. `zstd`：Zstandard 提供了较高的压缩比和较快的压缩速度，但相对于其他算法，解压缩速度较慢。

  compression.type 在生产者端和 broker 端设置，一般不建议设置 broker 端消息压缩类型，可能会造成预料外的压缩 / 解压缩操作导致 CPU 飙升

Kafka 的应答级别:

+ 0: 不需要任何应答，性能最高，但是消息丢失风险最高
+ 1: leader 写入成功后，会返回一个应答，性能和消息丢失风险居中
+ -1(all): leader 和 follower 都写入成功后，会返回一个应答，性能和消息丢失风险最低

默认分区器：

+ 有 key 时：Kafka 默认分区器会把 key 的 hashcode%topic 的 partition 数得到的值作为分区

+ 无 key 时：使用黏性分区器，随机选取一个分区，并尽可能一直使用该分区，直到该批次消息结束。下一轮消息会随机选取新的分区发送（和上次分区不同）

## AdminClient

AdminClinet 是用于对 Kafka 进行基本管理

### 基本使用

KafkaAdminDemo.java 演示了如何使用 admin client：

+ 列出所有 topic
+ 创建了名为 test 的 topic，两个分区、两个副本
+ 查看了名为 test 的分区和副本数
+ 删除了名为 test 的 topic

## produce

### 异步发送

KafkaClient 默认为异步发送消息

ProduceDemo.java 演示了如何使用 Kafka 发送一个异步消息，并指定消息回调方法

发送的消息指定了 key，默认分区器会把所有 key 相同的消息会落到同一个 partition 上

### 同步发送

ProduceSyncDemo.java 演示了使用 Kafka 发送一个消息，并使用 get 方法获取结果，使得这个消息变为同步

### At Least Once

可以最大程度得保证消息的可靠性，缺点是吞吐量较低，满足以下条件：

1. 生产者设置 ack 为 -1，让 leader 和 follower 响应

```java
properties.put(ProducerConfig.ACKS_CONFIG,"-1");
```

2. Topic 副本 >=2，创建 Topic 时，注意副本

```java
// 创建一个名为 test 的 topic, 2 个分区, 2 个副本
NewTopic newTopic=new NewTopic("test",2,(short)2);
```

3. Broker 配置 ISR 最小应答副本数（min.insync.replicas）>=2，修改 `server.properties`

```properties
min.insync.replicas=2
```

### At Most Once

至少一次，只需要把生产者 ack 设置为 0

```java
properties.put(ProducerConfig.ACKS_CONFIG,"0");
```

### Exactly Once

Exactly Once（精确一次），满足以下条件：

1. 实现 At Least Once（至少一次）

2. 开启幂等性，默认是开启的

   ```java
   properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
   ```

3. 开启事务

   开启事务的前提是开启幂等性，为了实现精确一次语义，Kafka 的生产者和消费者都需要使用事务功能，并且消费者需要正确处理消费位移的提交和恢复

幂等性：能保证消息在单 partition 内会话不重复，重复数据不会落盘，判断消息重复依据是 `<PID,Partition,SeqNumber>`

ProduceExactlyOnceDemo.java：演示了生产者端的 Exactly Once 实现

## consumer

Kafka 消费者通过 pull 的方式拉取数据

### 消费 Topic

ConsumerTopicDemo.java 演示了如何消费指定 topic 的消息，通过 pull 的方式拉取数据，每批次数据拉取间隔 1s

### 消费 Partition

ConsumerPartitionDemo.java 演示了如何消费指定 topic partition 的消息

### 消费者组

每个分区只会由消费者组中其中一个消费者消费，具有相同 `group.id` 的消费者处于同一个消费者组

```java
properties.put(ConsumerConfig.GROUP_ID_CONFIG,"test-topic-group0");
```

### commit offset

Kafka 客户端消费者有自动 offset 提交功能，也就是自动设置消费者对特定 partition 的消息位点，让消费者知道消费过哪些消息，相关参数如下：

+ enable.auto.commit 是否开启自动提交 offset 功能，默认是 true
+ auto.commit.interval.ms 直动提交 offset 的时间间隔，默认是 5s

如果要手动控制，可以把自动提交关闭，使用手动提交，手动提交会把当前批次 offset 最高的值进行提交

ConsumerOffsetCommitAsync.java 演示了如何使用异步方式提交 Offset，异步提交不会进行重试

ConsumerOffsetCommitSync.java 演示了如何使用同步方式提交 Offset，同步方式提交会阻塞当前线程，自动进行失败重试，直到提交成功

### offset 设置

Kafka 客户端消费者可以通过 `auto.offset.reset` 控制拉取策略，可选值如下：

+ earliest

  将 offset 设置为最早偏移量，从头开始消费

+ latest

  默认值，自动将偏移量设置为最后提交的偏移量，如果没有提交的偏移量，设置为最新偏移量

+ none

  如果未找到消费者组先前的偏移量，则抛出异常

ConsumerOffsetSeek.java 演示了重置 Topic partition 的 offset 到指定位置，从指定 offset 开始消费

ConsumerOffsetTimeSeek.java 演示了重置 Topic partition 的 offset 到指定时间，从某个时间点后开始消费

### Exactly Once

如果要实现 Consumer 端的 Exactly Once，需要将消费过程（Mysql 等）和提交 offset 做原子绑定

## 其他问题

### Kafak 消息的有序性

一般认为 kafka 单分区（partition）有序，多分区无序，但单分区内有序也有一定条件：

如果未开启幂等性，需要把客户端 `max.in.flight.requests.per.connection` 设置为 1，保证每个 connection 只会同时发一批消息，防止同时发多批消息时，由于部分消息失败导致的乱序

如果开启了幂等性，客户端配置 `max.in.flight.requests.per.connection` <=5，kafka 会缓存同一 connecttion 近 5 个 request
的元数据，即使部分消息失败，而重试成功，服务器也会将这几个消息重排序

### 消息积压

由于 Kafka 一个 partition 只由一个消费者消费，如果 Kafka 消费者能力不足，考虑增加 partition，增加 consumer，让 partition 数量 =consumer 数量

如果下游支持批处理，考虑提高每次 pull 消息的数量

### 磁盘选择

由于 Kakfa 的实现机制，写数据采取追加方式，基本为顺序读写，顺序读写固态和机械磁盘并不会有显著差异，使用固态并不会显著提升 Kafka 速度

### 磁盘空间预估

Kakfa 默认采取的策略是保存 7 天消息，实际上空间预估需要根据日志保存策略、副本数、冗余空间量（一般为 70%）、数据量决定

可以考虑按如下方式计算：` 每日条数 x 副本数 x 日志保留天数 / 0.7 `

### 内存设置

JVM 堆内存设置取决于 Kafka 集群实际吞吐量，建议压测并根据 GC 情况调整

### 线程配置

kafka 有几个参数和 cpu 线程有关

+ num.io.threads

  负责写磁盘的线程数，建议占总 CPU 核数的 50%

+ num.replica.fetchers

  副本拉取的线程数

+ num.network.threads

  用于网络传输的线程数

## 参考

https://www.bilibili.com/video/BV1vr4y1677k

[huaweicloudDocs/mrs (github.com)](https://github.com/huaweicloudDocs/mrs)