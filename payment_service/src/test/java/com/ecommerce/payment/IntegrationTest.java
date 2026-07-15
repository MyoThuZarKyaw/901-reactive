package com.ecommerce.payment;

import com.ecommerce.payment.config.AsyncSyncConfiguration;
import com.ecommerce.payment.config.DatabaseTestcontainer;
import com.ecommerce.payment.config.JacksonConfiguration;
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
        PaymentServiceApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.ecommerce.payment.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
