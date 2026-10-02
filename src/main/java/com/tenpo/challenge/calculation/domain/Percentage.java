package com.tenpo.challenge.calculation.domain;

import java.math.BigDecimal;

public record Percentage(BigDecimal value) {

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }
}
