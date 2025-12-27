package com.innowise.mapper;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.model.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentDto toDto(Payment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(CreatePaymentRequest dto);
}
