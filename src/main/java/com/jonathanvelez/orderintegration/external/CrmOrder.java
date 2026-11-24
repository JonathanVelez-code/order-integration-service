package com.jonathanvelez.orderintegration.external;

import java.math.BigDecimal;

/** An order as the CRM stores it. The CRM should always match the ERP. */
public record CrmOrder(
        String orderId,
        String customerId,
        BigDecimal amount,
        OrderStatus status) {

    public static CrmOrder from(ErpOrder erp) {
        return new CrmOrder(erp.orderId(), erp.customerId(), erp.amount(), erp.status());
    }
}
