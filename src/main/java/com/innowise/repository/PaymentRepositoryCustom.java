package com.innowise.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface PaymentRepositoryCustom {
    BigDecimal sumForPeriod(OffsetDateTime from, OffsetDateTime to);
}
