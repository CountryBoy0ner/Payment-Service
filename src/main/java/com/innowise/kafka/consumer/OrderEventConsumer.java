package com.innowise.kafka.consumer;


import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.kafka.event.CreateOrderEvent;
import com.innowise.kafka.event.CreatePaymentEvent;
import com.innowise.kafka.producer.PaymentEventProducer;
import com.innowise.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = "${app.kafka.topics.create-order}")
    @Transactional
    public void handle(CreateOrderEvent event) {
        log.info("Got create-order: {}", event);

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setOrderId(event.getOrderId());
        req.setUserId(event.getUserId());
        req.setPaymentAmount(event.getTotalAmount());
        req.setTimestamp(OffsetDateTime.now());
        PaymentDto payment = paymentService.create(req);
        CreatePaymentEvent out = new CreatePaymentEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus().name(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );
        paymentEventProducer.sendCreatePayment(out);
    }
}