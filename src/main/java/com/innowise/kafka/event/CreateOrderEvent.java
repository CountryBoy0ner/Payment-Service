package com.innowise.kafka.event;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderEvent {
    private Long orderId;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;

    public CreateOrderEvent(long l, long l1, String created) {
        this.orderId = l;
        this.userId = l1;
        this.status = created;
    }
}