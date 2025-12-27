package com.innowise.repository;

import com.innowise.model.Payment;
import com.innowise.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentRepositoryCustom {

    boolean existsByOrderId(Long orderId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);
}
