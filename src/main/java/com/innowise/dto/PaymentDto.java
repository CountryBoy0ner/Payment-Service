package com.innowise.dto;



import com.innowise.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PaymentDto {
    public Long id;
    public Long orderId;
    public Long userId;
    public PaymentStatus status;
    public OffsetDateTime timestamp;
    public BigDecimal paymentAmount;
}
