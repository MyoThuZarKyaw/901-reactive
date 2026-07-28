package com.ecommerce.order.service;

import com.ecommerce.order.web.rest.data.OrderRequest;
import com.ecommerce.order.web.rest.data.OrderResponse;
import org.springframework.transaction.annotation.Transactional;

public interface CustomOrderService extends OrderService {

    @Transactional
    OrderResponse createOrder(OrderRequest request);

}
