# spring-rocket

The Spring for [Apache RocketMQ](http://rocketmq.apache.org/) (spring-rocket) project applies core Spring concepts to the development of rocketMQ-based messaging solutions. It provides a "template" as a high-level abstraction for sending messages. It also provides support for Message-driven POJOs with @RocketListener annotations and a "listener container".

It is functionally similar to the following projects:

- Spring for Apache Kafka
- Spring for Apache Pulsar
- Spring AMQP

## Features

- [x] synchronous sending
- [x] asynchronous sending
- [ ] sequential sending
- [x] batch sending
- [ ] delay sending
- [ ] send in one-way mode
- [ ] send in transaction
- [ ] send and receive (request-reply)
- [x] concurrent consuming
- [ ] sequential consuming
- [x] batch consuming
- [x] consuming with push mode
- [ ] consuming with pull mode
- [x] message filtering
- [x] message tracing
- [x] authentication and authorization support

## Requirements

* Maven 3.0 and above
* Rocket Client 4.9 and above
* JDK 17 and above
* Spring 6.0 and above

## Modules

There are several modules in Spring for Apache RocketMQ. Here is a quick overview:

### spring-boot-autoconfigure-rocket

Aims to integrate Apache RocketMQ with [Spring Boot](http://projects.spring.io/spring-boot/).

### spring-rocket

The main library that provides the API to access Apache RocketMQ.

### spring-rocket-test

Provides utilities to help with testing Spring for Apache RocketMQ applications.

## Test

Build `spring-rocket` first, then build `spring-boot-autoconfigure-rocket`, and then you can run `spring-rocket-test`

## Usage

Add a dependency using maven:

* Spring:

```xml
<!--add dependency in pom.xml-->
<dependency>
    <groupId>org.springframework.rocket</groupId>
    <artifactId>spring-rocket</artifactId>
    <version>${RELEASE.VERSION}</version>
</dependency>
```

* Spring Boot:

```xml
<!--add dependency in pom.xml-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure-rocket</artifactId>
    <version>${RELEASE.VERSION}</version>
</dependency>
```

## License

Spring for Apache RocketMQ is Open Source software released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
