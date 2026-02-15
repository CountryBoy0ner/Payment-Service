package com.innowise.service;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface PaymentService {
    PaymentDto create(CreatePaymentRequest req);
    List<PaymentDto> getByOrderId(Long orderId);
    List<PaymentDto> getByUserId(Long userId);
    List<PaymentDto> getByStatuses(List<PaymentStatus> statuses);
    BigDecimal totalSum(OffsetDateTime from, OffsetDateTime to);

}
