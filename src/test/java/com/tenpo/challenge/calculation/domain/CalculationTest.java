package com.tenpo.challenge.calculation.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationTest {

    @Test
    void appliesPercentageMarkupToSum() {
        Calculation calculation = Calculation.of(
            BigDecimal.valueOf(5), BigDecimal.valueOf(5), new Percentage(BigDecimal.TEN));

        assertThat(calculation.result()).isEqualByComparingTo(BigDecimal.valueOf(11));
    }

    @Test
    void stripsTrailingZerosFromResult() {
        Calculation calculation = Calculation.of(
            BigDecimal.valueOf(5), BigDecimal.valueOf(5), new Percentage(BigDecimal.TEN));

        assertThat(calculation.result().toPlainString()).isEqualTo("11");
    }
}
