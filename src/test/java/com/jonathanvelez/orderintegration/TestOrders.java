package com.jonathanvelez.orderintegration;

import java.math.BigDecimal;
import java.time.Instant;

import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.external.OrderStatus;

/** Small helpers to build test orders. */
public final class TestOrders {

    public static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");

    private TestOrders() {
    }

    public static ErpOrder erpOrder(String orderId, String amount, OrderStatus status, int minutesAfterT0) {
        return new ErpOrder(orderId, "CUST-001", new BigDecimal(amount), status, T0.plusSeconds(60L * minutesAfterT0));
    }
}
