package com.ecommerce.order;

import com.ecommerce.order.service.dto.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class InventoryClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    public InventoryClient(RestClient.Builder restClientBuilder, ClientHttpRequestInterceptor tokenRelayInterceptor, DiscoveryClient discoveryClient) {
        this.restClient = restClientBuilder.requestInterceptor(tokenRelayInterceptor).build();
        this.discoveryClient = discoveryClient;
    }

    private String getProductsResourceUrl() {
        // return "http://inventory-service/api/products";
        List<ServiceInstance> instances = discoveryClient.getInstances("inventory-service");
        if (instances.isEmpty()) {
            throw new OrderServiceException("inventory-service is currently unavailable");
        }
        return instances.get(0).getUri() + "/api/products";
    }

    public List<ProductDTO> getProducts() {
        log.info("Calling Inventory Service to fetch products...");
        return restClient.get()
            .uri(getProductsResourceUrl())
            .retrieve()
            .body(new ParameterizedTypeReference<List<ProductDTO>>() {});
    }

    @CircuitBreaker(name = "inventory-service-getProducts", fallbackMethod = "isProductInStockFallback")
    @Retry(name = "inventory-service-getProducts")
    public boolean isProductInStock(String productId, Integer requestedQuantity) {
        List<ProductDTO> products = getProducts();
        if (products == null) return false;
        return products.stream()
            .filter(p -> p.getProductId().equalsIgnoreCase(productId))
            .findFirst()
            .map(p -> p.getStockQuantity() >= requestedQuantity)
            .orElse(false);
    }

    // Fallback method: called when circuit is OPEN or call fails
    public boolean isProductInStockFallback(String productId, Integer requestedQuantity, Exception e) {
        log.error("Inventory Service is unavailable. Fallback triggered for product {}. Error: {}", productId, e.getMessage());
        throw new OrderServiceException("Inventory system is currently unavailable. Please try again later.");
    }
}
