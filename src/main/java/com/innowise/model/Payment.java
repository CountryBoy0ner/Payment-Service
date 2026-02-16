package com.innowise.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private Long userId;

    @Field("status")
    private PaymentStatus status;

    @Field("timestamp")
    private OffsetDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;
}
