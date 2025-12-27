package com.innowise.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CreatePaymentRequest {

    @NotNull
    public Long orderId;

    @NotNull
    public Long userId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    public BigDecimal paymentAmount;

    public OffsetDateTime timestamp;
}