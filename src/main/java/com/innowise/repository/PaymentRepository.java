package com.innowise.repository;


import com.innowise.model.Payment;
import com.innowise.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderId(Long orderId);


    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    @Query("""
        select coalesce(sum(p.paymentAmount), 0)
        from Payment p
        where p.timestamp >= :from and p.timestamp <= :to
    """)
    BigDecimal sumForPeriod(OffsetDateTime from, OffsetDateTime to);
}
