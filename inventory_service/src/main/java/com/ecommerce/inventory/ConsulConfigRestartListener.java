package com.ecommerce.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsulConfigRestartListener {

    private final ConfigurableApplicationContext context;

    public ConsulConfigRestartListener(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onConfigChange(RefreshScopeRefreshedEvent event) {
        log.warn("[!!!] Detected Consul configuration change! Triggering Docker restart...");
        new Thread(() -> {
            try {
                // Short pause to allow open HTTP requests / logs to flush
                Thread.sleep(1000); // 1-second grace period
                SpringApplication.exit(context, () -> 0);
                System.exit(0);
            } catch (Exception e) {
                log.error("Error while exiting for restart", e);
                System.exit(1);
            }
        }).start();
    }
}
