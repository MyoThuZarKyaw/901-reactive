package com.ecommerce.gateway.web.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class CircuitBreakersFallbackController {

    @RequestMapping("/order-service")
    public Mono<ResponseEntity<Map<String, String>>> orderFallback() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Order Service is currently unavailable. Please try again later."))
        );
    }

    @RequestMapping("/inventory-service")
    public Mono<ResponseEntity<Map<String, String>>> inventoryFallback() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Inventory Service is currently unavailable. Please try again later."))
        );
    }

    @RequestMapping("/payment-service")
    public Mono<ResponseEntity<Map<String, String>>> paymentFallback() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Payment Service is currently unavailable. Please try again later."))
        );
    }

}
