package com.tenpo.challenge.calculation.infrastructure.adapter.in.web;

import com.tenpo.challenge.calculation.domain.Calculation;

import java.math.BigDecimal;

public record CalculationResponse(BigDecimal num1, BigDecimal num2, BigDecimal percentage, BigDecimal result) {

    public static CalculationResponse from(Calculation calculation) {
        return new CalculationResponse(
            calculation.num1(), calculation.num2(), calculation.percentage().value(), calculation.result());
    }
}
