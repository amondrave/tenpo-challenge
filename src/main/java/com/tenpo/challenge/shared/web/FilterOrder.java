package com.tenpo.challenge.shared.web;

import org.springframework.core.Ordered;

public final class FilterOrder {

    public static final int CALL_HISTORY = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int RATE_LIMIT = Ordered.HIGHEST_PRECEDENCE + 20;

    private FilterOrder() {
    }
}
