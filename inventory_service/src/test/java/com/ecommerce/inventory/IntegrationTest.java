package com.ecommerce.inventory;

import com.ecommerce.inventory.config.AsyncSyncConfiguration;
import com.ecommerce.inventory.config.DatabaseTestcontainer;
import com.ecommerce.inventory.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        InventoryServiceApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.ecommerce.inventory.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
