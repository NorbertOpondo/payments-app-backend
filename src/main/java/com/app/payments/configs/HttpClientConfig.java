package com.app.payments.configs;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Value("${payment.gateway.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${payment.gateway.read-timeout:10000}")
    private int readTimeout;

    @Value("${payment.gateway.mpesa.base-url}")
    private String mpesaBaseUrl;

    @Value("${payment.gateway.card.base-url}")
    private String cardBaseUrl;

    @Bean("mpesaRestClient")
    public RestClient mpesaRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(mpesaBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean("cardRestClient")
    public RestClient cardRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(cardBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(Duration.ofMillis(500), 2.0))
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        return factory;
    }
}
