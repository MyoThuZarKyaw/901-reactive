package com.ecommerce.order.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderItemTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static OrderItem getOrderItemSample1() {
        return new OrderItem().id(1L).orderItemId("orderItemId1").orderNumber("orderNumber1").productId("productId1");
    }

    public static OrderItem getOrderItemSample2() {
        return new OrderItem().id(2L).orderItemId("orderItemId2").orderNumber("orderNumber2").productId("productId2");
    }

    public static OrderItem getOrderItemRandomSampleGenerator() {
        return new OrderItem()
            .id(longCount.incrementAndGet())
            .orderItemId(UUID.randomUUID().toString())
            .orderNumber(UUID.randomUUID().toString())
            .productId(UUID.randomUUID().toString());
    }
}
