package com.innowise.kafka.producer;

import com.innowise.kafka.event.CreatePaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, CreatePaymentEvent> kafkaTemplate;

    @Value("${app.kafka.topics.create-payment}")
    private String topic;

    public void sendCreatePayment(CreatePaymentEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event)
                .whenComplete((res, ex) -> {
                    if (ex != null) log.error("Failed to publish create-payment", ex);
                    else log.info("Published create-payment: topic={} partition={} offset={}",
                            res.getRecordMetadata().topic(),
                            res.getRecordMetadata().partition(),
                            res.getRecordMetadata().offset());
                });
    }
}