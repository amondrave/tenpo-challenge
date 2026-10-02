package com.tenpo.challenge.calculation.infrastructure.config;

import com.tenpo.challenge.calculation.application.port.out.PercentageProviderPort;
import com.tenpo.challenge.calculation.application.service.CalculationService;
import com.tenpo.challenge.calculation.infrastructure.adapter.out.external.MockPercentageAdapter;
import com.tenpo.challenge.calculation.infrastructure.adapter.out.external.PercentageProviderProperties;
import com.tenpo.challenge.calculation.infrastructure.adapter.out.external.RetryingPercentageProviderAdapter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PercentageProviderProperties.class)
public class CalculationConfig {

    @Bean
    public MockPercentageAdapter mockPercentageAdapter(PercentageProviderProperties properties) {
        return new MockPercentageAdapter(properties);
    }

    @Bean
    public PercentageProviderPort percentageProviderPort(
            MockPercentageAdapter mockPercentageAdapter,
            RetryRegistry retryRegistry,
            PercentageProviderProperties properties) {
        Retry retry = retryRegistry.retry("percentageProvider");
        return new RetryingPercentageProviderAdapter(mockPercentageAdapter, retry, properties.timeout());
    }

    @Bean
    public CalculationService calculationService(PercentageProviderPort percentageProviderPort) {
        return new CalculationService(percentageProviderPort);
    }
}
