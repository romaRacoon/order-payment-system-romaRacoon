package dev.sorokin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentStubConfig {
    @Value("${payment.stub-url}")
    private String BASE_URL;

    @Value("${payment.payment.base-url}")
    private String PAYMENT_URL;

    @Value("${payment.warehouse.base-url}")
    private String WAREHOUSE_URL;

    @Bean
    public WebClient paymentStubClient() {
        return WebClient.builder()
                .baseUrl(BASE_URL + PAYMENT_URL)
                .build();
    }

    @Bean
    public WebClient warehouseStubClient() {
        return WebClient.builder()
                .baseUrl(BASE_URL + WAREHOUSE_URL)
                .build();
    }
}
