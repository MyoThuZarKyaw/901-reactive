package com.ecommerce.order.service.impl;

import com.ecommerce.order.InventoryClient;
import com.ecommerce.order.OrderServiceException;
import com.ecommerce.order.PaymentClient;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.CustomOrderService;
import com.ecommerce.order.service.OrderItemService;
import com.ecommerce.order.service.dto.OrderDTO;
import com.ecommerce.order.service.dto.OrderItemDTO;
import com.ecommerce.order.service.dto.PaymentDTO;
import com.ecommerce.order.service.mapper.OrderMapper;
import com.ecommerce.order.web.rest.data.OrderRequest;
import com.ecommerce.order.web.rest.data.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomOrderServiceImpl extends OrderServiceImpl implements CustomOrderService {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderItemService orderItemService;

    public CustomOrderServiceImpl(
        OrderRepository orderRepository,
        OrderMapper orderMapper,
        InventoryClient inventoryClient,
        OrderItemService orderItemService,
        PaymentClient paymentClient) {
        super(orderRepository, orderMapper);
        this.inventoryClient = inventoryClient;
        this.orderItemService = orderItemService;
        this.paymentClient = paymentClient;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        for (OrderItemDTO item : request.getOrderItems()) {
            boolean inStock = inventoryClient.isProductInStock(item.getProductId(), item.getQuantity().intValue());

            if (!inStock) {
                throw new OrderServiceException("Product " + item.getProductId() + " is out of stock or insufficient quantity!");
            }
        }

        var order = new OrderDTO();
        order.setOrderDate(OffsetDateTime.now().toInstant());
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("COMPLETED");
        order.setTotalAmount(request.getOrderItems().stream().map(OrderItemDTO::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        order = this.save(order);

        List<OrderItemDTO> orderItems = new ArrayList<>();
        for (OrderItemDTO item : request.getOrderItems()) {
            item.setOrderItemId("ITEM-" + System.currentTimeMillis());
            item.setOrderNumber(order.getOrderNumber());

            var orderItem = orderItemService.save(item);
            orderItems.add(orderItem);
        }

        var payment = new PaymentDTO();
        payment.setAmount(order.getTotalAmount());
        payment.setOrderNumber(order.getOrderNumber());
        payment.setPaymentDate(OffsetDateTime.now().toInstant());
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN-" + System.currentTimeMillis());
        paymentClient.createPayment(payment);

        return new OrderResponse(order, orderItems);
    }

}
