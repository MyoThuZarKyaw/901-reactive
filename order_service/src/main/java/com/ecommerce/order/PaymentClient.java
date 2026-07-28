package com.ecommerce.order;

import com.ecommerce.order.service.dto.PaymentDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Service
@Slf4j
public class PaymentClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    public PaymentClient(RestClient.Builder restClientBuilder, ClientHttpRequestInterceptor tokenRelayInterceptor, DiscoveryClient discoveryClient) {
        this.restClient = restClientBuilder.requestInterceptor(tokenRelayInterceptor).build();
        this.discoveryClient = discoveryClient;
    }

    private String getPaymentResourceUrl() {
//        return "http://payment-service/api/payments";
        List<ServiceInstance> instances = discoveryClient.getInstances("payment-service");
        if (instances.isEmpty()) {
            throw new OrderServiceException("payment-service is currently unavailable");
        }
        return instances.get(0).getUri() + "/api/payments";
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "createPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        log.info("Processing payment for order: {}", paymentDTO.getOrderNumber());
        return restClient.post()
            .uri(getPaymentResourceUrl())
            .body(paymentDTO)
            .retrieve()
            .body(PaymentDTO.class);
    }

    public PaymentDTO createPaymentFallback(PaymentDTO paymentDTO, Exception e) {
        log.error("Payment Service failed. giError: {}", e.getMessage());
        throw new OrderServiceException("Payment Service is currently unavailable. Please try again later.");
    }

    public PaymentDTO getPaymentById(Long id) {
        try {
            return restClient.get().uri(getPaymentResourceUrl() + "/" + id).retrieve().body(PaymentDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new OrderServiceException("Exception while getting payment", e);
        }
    }

    public boolean deletePayment(Long id) {
        try {
            restClient.delete().uri(getPaymentResourceUrl() + "/" + id).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
