package com.tenpo.challenge.calculation.infrastructure.adapter.out.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "percentage-provider")
public record PercentageProviderProperties(BigDecimal value, FailureMode failureMode, Duration timeout) {
}
