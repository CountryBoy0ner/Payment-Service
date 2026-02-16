package com.innowise.service.impl;

import com.innowise.dto.PaymentDto;
import com.innowise.service.PaymentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PaymentSecurityService {

    private final PaymentService service;

    public PaymentSecurityService(PaymentService service) {
        this.service = service;
    }

    public PaymentDto checkPaymentAccess(PaymentDto dto, Long currentUserId, String rolesHeader) {
        if (rolesHeader != null && rolesHeader.contains("ADMIN")) return dto;
        if (!dto.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Forbidden");
        }
        return dto;
    }
}