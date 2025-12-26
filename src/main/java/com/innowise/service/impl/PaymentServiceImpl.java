package com.innowise.service.impl;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.exception.PaymentAlreadyExistsException;
import com.innowise.integration.RandomNumberClient;
import com.innowise.mapper.PaymentMapper;
import com.innowise.model.Payment;
import com.innowise.model.PaymentStatus;
import com.innowise.repository.PaymentRepository;
import com.innowise.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repo;
    private final PaymentMapper mapper;
    private final RandomNumberClient randomClient;

    @Override
    @Transactional
    public PaymentDto create(CreatePaymentRequest dto) {
        if (repo.existsByOrderId(dto.getOrderId())) {
            throw new PaymentAlreadyExistsException(dto.getOrderId());
        }

        Payment p = mapper.toEntity(dto);
        p.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : OffsetDateTime.now());

        int n = randomClient.getRandomNumber();
        p.setStatus((n % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        return mapper.toDto(repo.save(p));
    }

    @Transactional
    @Override
    public List<PaymentDto> getByOrderId(Long orderId) {
        return repo.findByOrderId(orderId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    @Override
    public List<PaymentDto> getByUserId(Long userId) {
        return repo.findByUserId(userId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    @Override
    public List<PaymentDto> getByStatuses(List<PaymentStatus> statuses) {
        return repo.findByStatusIn(statuses).stream().map(mapper::toDto).toList();
    }

    @Transactional
    @Override
    public BigDecimal totalSum(OffsetDateTime from, OffsetDateTime to) {
        return repo.sumForPeriod(from, to);
    }
}
