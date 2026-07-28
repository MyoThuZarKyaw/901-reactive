package com.ecommerce.order.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class ClientConfig {

    @Bean
    public ClientHttpRequestInterceptor tokenRelayInterceptor() { // To get token from request header
        return (request, body, execution) -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest incomingRequest = attributes.getRequest();
                String authHeader = incomingRequest.getHeader("Authorization");
                if (authHeader != null) {
                    request.getHeaders().set("Authorization", authHeader);
                }
            }
            return execution.execute(request, body);
        };
    }

//    @Bean
//    @LoadBalanced // Tells Spring to connect Consul and resolve Service names (e.g: payment-service) by itself automatically.
//    public RestClient.Builder restClientBuilder() {
//        // Recreating the RestClient.Builder like this, make spring-boot-starter-opentelemetry loose the track of
//        // trace context id while invoking the chain of api calls to the other microservices. As the result of it, it
//        // generates new trace id and register them as separate traces into Raeger even thouth they belong to the
//        // single group of trace.
//        return RestClient.builder()
//            .requestInterceptor(tokenRelayInterceptor());
//    }

}
