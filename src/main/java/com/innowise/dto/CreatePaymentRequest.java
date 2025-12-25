package com.innowise.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CreatePaymentRequest {
    public Long orderId;
    public Long userId;
    public String status;           // например: NEW / PAID / FAILED
    public OffsetDateTime timestamp; // optional
    public BigDecimal paymentAmount;
}