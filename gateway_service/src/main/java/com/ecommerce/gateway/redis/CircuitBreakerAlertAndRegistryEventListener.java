package com.ecommerce.gateway.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
/*
 * References:
 *  - https://docs.spring.io/spring-data/redis/reference/4.0/redis/connection-modes.html
 *  - https://docs.spring.io/spring-data/redis/reference/4.0/redis/pubsub.html
 *  - https://docs.spring.io/spring-boot/4.0/appendix/application-properties/index.html#application-properties.data.spring.data.redis.port
 */
public class CircuitBreakerAlertAndRegistryEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${app_service.circuit.breaker.alert.action.enable:true}")
    private boolean alertActionEnabled;

    @Value("${app_service.circuit.breaker.alert.action.trip-cooldown-seconds:10}")
    private long tripCooldownSeconds;

    @Value("${app_service.circuit.breaker.alert.action.min-confidence:0.85}")
    private float minConfidence;

    private final ConcurrentMap<String, BreakerStatus> breakerStates = new ConcurrentHashMap<>();

    public record BreakerStatus(CircuitBreaker.State state, ZonedDateTime stateChangeTime) {}

    public Optional<BreakerStatus> getStatus(String circuitBreakerName) {
        return Optional.ofNullable(breakerStates.get(circuitBreakerName));
    }

    public Optional<CircuitBreaker.State> getState(String circuitBreakerName) {
        return getStatus(circuitBreakerName).map(BreakerStatus::state);
    }

    public Map<String, BreakerStatus> getAllStatuses() {
        return Map.copyOf(breakerStates);
    }

    @PostConstruct
    void init() {
        // Prime map + attach to breakers already created before registry listener registration.
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::attachStateListener);
    }

    private void attachStateListener(CircuitBreaker circuitBreaker) {
        breakerStates.putIfAbsent(
            circuitBreaker.getName(),
            new BreakerStatus(circuitBreaker.getState(), ZonedDateTime.now())
        );
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            breakerStates.put(
                event.getCircuitBreakerName(),
                new BreakerStatus(event.getStateTransition().getToState(), event.getCreationTime())
            );
            // This prints EVERY state change (e.g., CLOSED_TO_OPEN, OPEN_TO_HALF_OPEN, HALF_OPEN_TO_CLOSED)
            log.info("Circuit Breaker '{}' changed state: {} at {}",
                event.getCircuitBreakerName(),
                event.getStateTransition(),
                event.getCreationTime());
        });
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String alertJson = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received a circuit breaker alert: {}", alertJson);
            CircuitBreakerAlert alert = objectMapper.readValue(alertJson, CircuitBreakerAlert.class);
            if (alertActionEnabled
                        && "FAILED".equalsIgnoreCase(alert.getStatus())
                        && StringUtils.isNotBlank(alert.getTargetService())) {
                if (alert.getConfidence() < minConfidence) {
                    log.info("Skipping alert action: confidence={} below threshold={} for target={}",
                        alert.getConfidence(), minConfidence, alert.getTargetService());
                    return;
                }
                String serviceName = alert.getTargetService(); // e.g., "payment-service"
                ZonedDateTime receivedAt = ZonedDateTime.now();
                // Find ALL circuit breakers starting with this service name
                // Service breaker instance names will be in pattern {service-name}-{operation} (e.g., payment-service-createPayment)
                circuitBreakerRegistry.getAllCircuitBreakers().stream()
                    .filter(cb -> cb.getName().startsWith(serviceName))
                    .forEach(cb -> {
                        BreakerStatus tracked = breakerStates.get(cb.getName());
                        CircuitBreaker.State current = tracked != null ? tracked.state() : cb.getState();
                        if (current != CircuitBreaker.State.CLOSED) {
                            log.info("Skipping trip for circuit breaker: {} (state={})", cb.getName(), current);
                            return;
                        }
                        ZonedDateTime changedAt = tracked != null ? tracked.stateChangeTime() : null;
                        if (tripCooldownSeconds > 0
                                && changedAt != null
                                && Duration.between(changedAt, receivedAt).getSeconds() <= tripCooldownSeconds) {
                            log.info("Skipping trip for circuit breaker: {} (state={}, changedAt={}, only {}s ago, cooldown={}s)",
                                cb.getName(), current, changedAt,
                                Duration.between(changedAt, receivedAt).getSeconds(), tripCooldownSeconds);
                            return;
                        }
                        log.info("Tripping circuit breaker: {} (state={}, changedAt={})", cb.getName(), current, changedAt);
                        cb.transitionToOpenState();
                    });
            }
        } catch (IOException e) {
            log.error("Error while processing the message", e);
        }
    }

    public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
        attachStateListener(entryAddedEvent.getAddedEntry());
    }

    public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
        breakerStates.remove(entryRemoveEvent.getRemovedEntry().getName());
    }

    public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
        attachStateListener(entryReplacedEvent.getNewEntry());
    }

}
