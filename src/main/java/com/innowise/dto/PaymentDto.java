package com.innowise.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PaymentDto {
    public Long id;
    public Long orderId;
    public Long userId;
    public String status;
    public OffsetDateTime timestamp;
    public BigDecimal paymentAmount;
}