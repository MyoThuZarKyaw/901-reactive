package com.ecommerce.order.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;
    private String productId;
    private String name;
    private String description;
    private Integer stockQuantity;
    private BigDecimal unitPrice;
}
