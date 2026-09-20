package com.ecommerce.order.redis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerAlert {

    @JsonProperty("target_service")
    private String targetService;
    private String status;
    private Instant timestamp;

}
