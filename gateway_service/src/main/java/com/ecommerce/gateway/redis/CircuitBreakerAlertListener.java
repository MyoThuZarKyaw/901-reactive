package com.ecommerce.gateway.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
/*
 * References:
 *  - https://docs.spring.io/spring-data/redis/reference/4.0/redis/connection-modes.html
 *  - https://docs.spring.io/spring-data/redis/reference/4.0/redis/pubsub.html
 *  - https://docs.spring.io/spring-boot/4.0/appendix/application-properties/index.html#application-properties.data.spring.data.redis.port
 */
public class CircuitBreakerAlertListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        try {
            String alertJson = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received a circuit breaker alert: {}", alertJson);
            CircuitBreakerAlert alert = objectMapper.readValue(alertJson, CircuitBreakerAlert.class);
            if(("DEGRADED".equalsIgnoreCase(alert.getStatus()) || "FAILED".equalsIgnoreCase(alert.getStatus()))
                        && StringUtils.isNotBlank(alert.getTargetService())) {
                String serviceName = alert.getTargetService(); // e.g., "payment-service"
                // Find ALL circuit breakers starting with this service name
                // Service breaker instance names will be in pattern {service-name}-{operation} (e.g., payment-service-createPayment)
                circuitBreakerRegistry.getAllCircuitBreakers().stream()
                    .filter(cb -> cb.getName().startsWith(serviceName))
                    .forEach(cb -> {
                        log.info("Tripping circuit breaker: {}", cb.getName());
                        cb.transitionToOpenState();
                    });

//                circuitBreakerRegistry
//                    .circuitBreaker("paymentServiceClient")
//                    .transitionToOpenState();
            }
        } catch (IOException e) {
            log.error("Error while processing the message", e);
        }
    }

}
