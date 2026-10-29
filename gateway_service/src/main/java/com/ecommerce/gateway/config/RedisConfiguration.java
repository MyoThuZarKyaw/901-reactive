package com.ecommerce.gateway.config;

import com.ecommerce.gateway.redis.CircuitBreakerAlertAndRegistryEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;


@Configuration
public class RedisConfiguration {

    public static final String CIRCUIT_BREAKER_CHANNEL = "circuit-breaker-channel";

//    @Value("${spring.data.redis.host:localhost}")
//    private String redisHost;
//
//    @Value("${spring.data.redis.port:6379}")
//    private int redisPort;
//
//    @Bean
//    public RedisConnectionFactory lettuceConnectionFactory() {
//        // Pass the host and port so it reads from environment variables!
//        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
//        return new LettuceConnectionFactory(serverConfig);
//    }

    @Bean
    MessageListenerAdapter messageListenerAdapter(CircuitBreakerAlertAndRegistryEventListener listener) {
        return new MessageListenerAdapter(listener);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, ChannelTopic.of(CIRCUIT_BREAKER_CHANNEL));
        return container;
    }

}
