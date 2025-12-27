package com.innowise.kafka.event;

import com.innowise.model.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentEvent {
    private String paymentId;
    private Long orderId;
    private Long userId;
    private String status;
    private BigDecimal paymentAmount;
    private OffsetDateTime timestamp;

    public CreatePaymentEvent(Long orderId,
                              Long userId,
                              PaymentStatus status,
                              BigDecimal paymentAmount,
                              OffsetDateTime timestamp
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status.name();
        this.paymentAmount = paymentAmount;
        this.timestamp = timestamp;
    }

}
