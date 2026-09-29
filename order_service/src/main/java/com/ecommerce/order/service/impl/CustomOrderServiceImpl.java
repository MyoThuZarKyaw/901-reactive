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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;


@Service
@Slf4j
public class CustomOrderServiceImpl extends OrderServiceImpl implements CustomOrderService {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderItemService orderItemService;

    public CustomOrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper,
                                  InventoryClient inventoryClient, OrderItemService orderItemService,
                                  PaymentClient paymentClient) {
        super(orderRepository, orderMapper);
        this.inventoryClient = inventoryClient;
        this.orderItemService = orderItemService;
        this.paymentClient = paymentClient;
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        // Stock Check
        for (OrderItemDTO item : request.getOrderItems()) {
            // The Circuit Breaker and Retry are inside this client call
            if (!inventoryClient.isProductInStock(item.getProductId(), item.getQuantity().intValue())) {
                throw new OrderServiceException("Product " + item.getProductId() + " is out of stock!");
            }
        }

        // Save Order as PENDING
        OrderDTO order = saveInitialOrderAndItems(request);

        // Process Payment
        try {
            processPayment(order);
            // Update to COMPLETED after payment success
            order = updateOrderStatus(order.getId(), "COMPLETED");
        } catch (Exception e) {
            order = handlePaymentFailure(order.getId());
            log.error("Payment failed. Order is marked as PAYMENT_FAILED.");
            //throw new OrderServiceException("Payment failed. Order is marked as PAYMENT_FAILED.");
        }

        return new OrderResponse(order, request.getOrderItems());
    }

    @Transactional
    public OrderDTO saveInitialOrderAndItems(OrderRequest request) {
        var order = new OrderDTO();
        order.setOrderDate(OffsetDateTime.now().toInstant());
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING_PAYMENT");
        order.setTotalAmount(request.getOrderItems().stream()
            .map(OrderItemDTO::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        OrderDTO savedOrder = this.save(order);

        for (OrderItemDTO item : request.getOrderItems()) {
            item.setOrderItemId("ITEM-" + System.currentTimeMillis());
            item.setOrderNumber(savedOrder.getOrderNumber());
            orderItemService.save(item);
        }
        return savedOrder;
    }

    private void processPayment(OrderDTO order) {
        var payment = new PaymentDTO();
        payment.setAmount(order.getTotalAmount());
        payment.setOrderNumber(order.getOrderNumber());
        payment.setPaymentDate(OffsetDateTime.now().toInstant());
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN-" + System.currentTimeMillis());
        // The Circuit Breaker and Retry are inside this client call
        paymentClient.createPayment(payment);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        OrderDTO statusPatch = new OrderDTO();
        statusPatch.setId(id);
        statusPatch.setStatus(status);

        Optional<OrderDTO> result = this.partialUpdate(statusPatch);
        log.info("Order ID {} status successfully updated to {}", id, status);
        return result.orElse(null);
    }

    @Transactional
    public OrderDTO handlePaymentFailure(Long orderId) {
        return updateOrderStatus(orderId, "PAYMENT_FAILED");
    }
}
