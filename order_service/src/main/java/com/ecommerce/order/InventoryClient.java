package com.ecommerce.order;

import com.ecommerce.order.service.dto.ProductDTO;
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
//        return "http://inventory-service/api/products";
        List<ServiceInstance> instances = discoveryClient.getInstances("inventory-service");
        if (instances.isEmpty()) {
            throw new OrderServiceException("inventory-service is currently unavailable");
        }
        return instances.get(0).getUri() + "/api/products";
    }

    /**
     * GET: Fetches all products synchronously
     */
    public List<ProductDTO> getProducts() {
        try {
            return restClient.get()
                .uri(getProductsResourceUrl())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductDTO>>() {}); // Safely maps to List<ProductDTO> without reactive steps
        } catch (Exception e) {
            throw new OrderServiceException("Exception while getting products from Inventory Service.", e);
        }
    }

    /**
     * Checks if a specific product has enough stock
     */
    public boolean isProductInStock(String productId, Integer requestedQuantity) {
        List<ProductDTO> products = getProducts();
        if (products == null) return false;

        return products.stream()
            .filter(p -> p.getProductId().equalsIgnoreCase(productId))
            .findFirst()
            .map(p -> p.getStockQuantity() >= requestedQuantity)
            .orElse(false);
    }
}
