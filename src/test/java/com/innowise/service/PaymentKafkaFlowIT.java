package com.innowise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.kafka.event.CreateOrderEvent;
import com.innowise.model.Payment;
import com.innowise.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentKafkaFlowIT {


    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }


        @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> wiremock = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.13.2"))
            .withExposedPorts(8080);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);

        r.add("SPRING_DATA_MONGODB_URI", mongo::getReplicaSetUrl);

        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        r.add("user-service.base-url", () ->
                "http://" + wiremock.getHost() + ":" + wiremock.getFirstMappedPort()
        );
    }



    @BeforeAll
    static void stubRandomApi() throws Exception {
        String base = "http://" + wiremock.getHost() + ":" + wiremock.getMappedPort(8080);

        String mappingJson = """
                {
                  "request": { "method": "GET", "urlPathPattern": "/.*" },
                  "response": {
                    "status": 200,
                    "headers": { "Content-Type": "application/json" },
                    "body": "96"
                  }
                }
                """;


        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/__admin/mappings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mappingJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("WireMock mapping failed: " + resp.statusCode() + " " + resp.body());
        }
    }

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createOrderEvent_shouldCreatePaymentInMongo_andPublishCreatePaymentEvent() {
        long orderId = 1L;
        long userId = 3L;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("create-payment"));
            consumer.poll(Duration.ofMillis(200));

            CreateOrderEvent event = new CreateOrderEvent(
                    orderId,
                    userId,
                    "NEW",
                    LocalDateTime.now(),
                    new java.math.BigDecimal("9.99")
            );
            kafkaTemplate.send("create-order", String.valueOf(orderId), event);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        List<Payment> found = paymentRepository.findByOrderId(orderId);
                        assertFalse(found.isEmpty(), "Payment must be created in MongoDB");
                    });

            ConsumerRecord<String, String> record = pollOne(consumer, Duration.ofSeconds(15));
            assertNotNull(record, "Must publish event to create-payment topic");
            assertNotNull(record.value());

        }
    }

    private static ConsumerRecord<String, String> pollOne(KafkaConsumer<String, String> c, Duration max) {
        long deadline = System.currentTimeMillis() + max.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> recs = c.poll(Duration.ofMillis(300));
            for (ConsumerRecord<String, String> r : recs) return r;
        }
        return null;
    }
}
