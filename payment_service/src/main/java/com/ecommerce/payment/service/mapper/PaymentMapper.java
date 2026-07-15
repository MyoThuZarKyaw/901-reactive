package com.ecommerce.payment.service.mapper;

import com.ecommerce.payment.domain.Payment;
import com.ecommerce.payment.service.dto.PaymentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Payment} and its DTO {@link PaymentDTO}.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper extends EntityMapper<PaymentDTO, Payment> {}
