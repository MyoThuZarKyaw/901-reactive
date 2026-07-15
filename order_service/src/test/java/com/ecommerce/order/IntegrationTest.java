package com.ecommerce.order;

import com.ecommerce.order.config.AsyncSyncConfiguration;
import com.ecommerce.order.config.DatabaseTestcontainer;
import com.ecommerce.order.config.JacksonConfiguration;
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
        OrderServiceApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.ecommerce.order.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
