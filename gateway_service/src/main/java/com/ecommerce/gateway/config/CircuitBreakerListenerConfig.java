package com.ecommerce.gateway.config;

import com.ecommerce.gateway.redis.CircuitBreakerAlertAndRegistryEventListener;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircuitBreakerListenerConfig {

    private final CircuitBreakerRegistry registry;
    private final CircuitBreakerAlertAndRegistryEventListener listener;

    @PostConstruct
    void register() {
        registry.getEventPublisher()
            .onEntryAdded(listener::onEntryAddedEvent)
            .onEntryRemoved(listener::onEntryRemovedEvent)
            .onEntryReplaced(listener::onEntryReplacedEvent);
    }

}
