package com.tenpo.challenge.calculation.domain;

import java.math.BigDecimal;

public record Calculation(BigDecimal num1, BigDecimal num2, Percentage percentage, BigDecimal result) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static Calculation of(BigDecimal num1, BigDecimal num2, Percentage percentage) {
        BigDecimal sum = num1.add(num2);
        BigDecimal markup = sum.multiply(percentage.value()).divide(HUNDRED);
        BigDecimal result = sum.add(markup).stripTrailingZeros();
        return new Calculation(num1, num2, percentage, result);
    }
}
