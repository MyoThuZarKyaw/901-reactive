package com.ecommerce.order;

import com.ecommerce.order.service.dto.PaymentDTO;
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

    /**
     * CREATE: Sends a POST request to create a payment
     */
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        try {
            return restClient.post()
                .uri(getPaymentResourceUrl())
                .body(paymentDTO)
                .retrieve()
                .body(PaymentDTO.class); // No more reactive .block() required!
        } catch (Exception e) {
            throw new OrderServiceException("Exception while creating payment", e);
        }
    }

    /**
     * GET: Fetches a payment by its ID
     */
    public PaymentDTO getPaymentById(Long id) {
        try {
            return restClient.get()
                .uri(getPaymentResourceUrl() + "/" + id)
                .retrieve()
                .body(PaymentDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            // Replaces WebClientResponseException.NotFound gracefully
            return null;
        } catch (Exception e) {
            throw new OrderServiceException("Exception while getting payment", e);
        }
    }

    /**
     * DELETE: Removes a payment record
     */
    public boolean deletePayment(Long id) {
        try {
            restClient.delete()
                .uri(getPaymentResourceUrl() + "/" + id)
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            // Logs 4xx and 5xx errors smoothly
            return false;
        } catch (Exception e) {
            throw new OrderServiceException("Exception while deleting payment", e);
        }
    }
}
