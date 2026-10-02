package com.tenpo.challenge.history.infrastructure.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "history.capture")
public record HistoryCaptureProperties(DataSize maxRequestBytes, DataSize maxResponseBytes) {
}
