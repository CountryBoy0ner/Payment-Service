package com.innowise.service.impl;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.integration.RandomNumberClient;
import com.innowise.kafka.event.CreatePaymentEvent;
import com.innowise.kafka.producer.PaymentEventProducer;
import com.innowise.mapper.PaymentMapper;
import com.innowise.model.Payment;
import com.innowise.model.PaymentStatus;
import com.innowise.repository.PaymentRepository;
import com.innowise.service.PaymentService;
import com.mongodb.DuplicateKeyException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repo;
    private final PaymentMapper mapper;
    private final RandomNumberClient randomClient;
    private final PaymentEventProducer producer;

    @Override
    public PaymentDto create(CreatePaymentRequest dto) {

        var existing = repo.findByOrderId(dto.getOrderId()).stream().findFirst().orElse(null);
        if (existing != null) {
            producer.sendCreatePayment(toEvent(existing));
            return mapper.toDto(existing);
        }

        Payment p = mapper.toEntity(dto);
        p.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : OffsetDateTime.now());

        int n = randomClient.getRandomNumber();
        p.setStatus((n % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        Payment saved;
        try {
            saved = repo.save(p);
        } catch (DuplicateKeyException e) {
            var already = repo.findByOrderId(dto.getOrderId()).stream().findFirst().orElse(null);
            if (already != null) {
                producer.sendCreatePayment(toEvent(already));
                return mapper.toDto(already);
            }
            throw e;
        }

        producer.sendCreatePayment(toEvent(saved));

        return mapper.toDto(saved);
    }

    private CreatePaymentEvent toEvent(Payment p) {
        return new CreatePaymentEvent(
                p.getId(),
                p.getOrderId(),
                p.getUserId(),
                p.getStatus().name(),
                p.getPaymentAmount(),
                p.getTimestamp()
        );
    }

    @Override
    public List<PaymentDto> getByOrderId(Long orderId) {
        return repo.findByOrderId(orderId).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<PaymentDto> getByUserId(Long userId) {
        return repo.findByUserId(userId).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<PaymentDto> getByStatuses(List<PaymentStatus> statuses) {
        return repo.findByStatusIn(statuses).stream().map(mapper::toDto).toList();
    }

    @Override
    public BigDecimal totalSum(OffsetDateTime from, OffsetDateTime to) {
        return repo.sumForPeriod(from, to);
    }
}
