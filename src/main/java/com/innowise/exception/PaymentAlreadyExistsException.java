package com.innowise.exception;

public class PaymentAlreadyExistsException extends RuntimeException {
    public PaymentAlreadyExistsException(Long orderId) {
        super("Payment for orderId=" + orderId + " already exists");
    }
}