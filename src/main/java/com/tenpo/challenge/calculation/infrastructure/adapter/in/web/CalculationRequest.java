package com.tenpo.challenge.calculation.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CalculationRequest(@NotNull BigDecimal num1, @NotNull BigDecimal num2) {
}
