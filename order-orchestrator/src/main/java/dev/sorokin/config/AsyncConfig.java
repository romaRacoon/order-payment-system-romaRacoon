package dev.sorokin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {
    @Bean("ioExecutor")
    public ExecutorService ioExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
