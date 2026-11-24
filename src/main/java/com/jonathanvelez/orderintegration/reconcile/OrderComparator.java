package com.jonathanvelez.orderintegration.reconcile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyType;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.ErpOrder;

/** Compares one ERP order with the CRM's copy. Pure logic, no Spring, easy to unit test. */
public final class OrderComparator {

    private OrderComparator() {
    }

    public static List<Discrepancy> compare(ErpOrder erp, Optional<CrmOrder> crmCopy, Instant now) {
        if (crmCopy.isEmpty()) {
            return List.of(new Discrepancy(erp.orderId(), DiscrepancyType.MISSING_IN_CRM,
                    erp.amount().toPlainString() + " " + erp.status(), null, now));
        }
        CrmOrder crm = crmCopy.get();
        List<Discrepancy> found = new ArrayList<>();
        if (!erp.customerId().equals(crm.customerId())) {
            found.add(new Discrepancy(erp.orderId(), DiscrepancyType.CUSTOMER_MISMATCH,
                    erp.customerId(), crm.customerId(), now));
        }
        // compareTo, not equals: BigDecimal.equals treats 10.0 and 10.00 as different
        if (erp.amount().compareTo(crm.amount()) != 0) {
            found.add(new Discrepancy(erp.orderId(), DiscrepancyType.AMOUNT_MISMATCH,
                    erp.amount().toPlainString(), crm.amount().toPlainString(), now));
        }
        if (erp.status() != crm.status()) {
            found.add(new Discrepancy(erp.orderId(), DiscrepancyType.STATUS_MISMATCH,
                    erp.status().name(), crm.status().name(), now));
        }
        return found;
    }
}
