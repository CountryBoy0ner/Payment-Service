package com.innowise.controller;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.model.PaymentStatus;
import com.innowise.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public PaymentDto create(@Valid @RequestBody CreatePaymentRequest req) {
        return service.create(req);
    }

    @GetMapping("/by-order/{orderId}")
    public List<PaymentDto> byOrder(@PathVariable Long orderId) {
        return service.getByOrderId(orderId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-user/{userId}")
    public List<PaymentDto> byUser(@PathVariable Long userId) {
        return service.getByUserId(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-status")
    public List<PaymentDto> byStatus(@RequestParam List<PaymentStatus> statuses) {
        return service.getByStatuses(statuses);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/total")
    public BigDecimal total(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return service.totalSum(from, to);
    }

    @GetMapping("/me")
    public List<PaymentDto> getMyPayments(
            @RequestHeader("X-User-Id") Long currentUserId
    ) {
        return service.getByUserId(currentUserId);
    }

}
