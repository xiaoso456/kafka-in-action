## 简介
## Kafka基本使用
### producer client
kafka 生产者发送一个消息通常会经过拦截器（Interceptor）、序列化器（Serializer）、分区器（Partitioner）

```mermaid
graph LR

producer-->interceptor-->serializer-->partitioner
```
Kafka producer一些常用参数如下:

+ batch.size

  数据积累到batch.size后，sender发送数据。默认值为16K
+ linger.ms: 

  消息最大发送延迟，默认值为0ms。如果设置5ms，当消息大小没有达到16KB（batch.size）时，如果5ms内没有新的消息发送，也会触发消息发送

Kafka的应答级别:
+ 0: 不需要任何应答，性能最高，但是消息丢失风险最高
+ 1: leader写入成功后，会返回一个应答，性能和消息丢失风险居中
+ -1(all): leader和follower都写入成功后，会返回一个应答，性能和消息丢失风险最低
## 参考

https://www.bilibili.com/video/BV1vr4y1677k