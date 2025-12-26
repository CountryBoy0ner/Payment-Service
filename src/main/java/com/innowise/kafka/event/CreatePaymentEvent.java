package com.innowise.kafka.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String status;
    private BigDecimal paymentAmount;
    private OffsetDateTime timestamp;
}