package com.ecommerce.order.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private BigDecimal amount;
    private String orderNumber;
    private Instant paymentDate;
    private String status;
    private String transactionId;

}
