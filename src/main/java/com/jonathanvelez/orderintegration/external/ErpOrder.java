package com.jonathanvelez.orderintegration.external;

import java.math.BigDecimal;
import java.time.Instant;

/** An order as the ERP (the system of record) reports it. */
public record ErpOrder(
        String orderId,
        String customerId,
        BigDecimal amount,
        OrderStatus status,
        Instant updatedAt) {
}
