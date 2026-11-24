package com.jonathanvelez.orderintegration.reconcile;

import java.util.List;

import org.springframework.batch.core.SkipListener;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.external.ErpOrder;

/**
 * If the CRM is still down for an order after the retries, the job skips that order
 * instead of failing the whole run, and audits the skip so it can be followed up.
 */
public class CrmSkipListener implements SkipListener<ErpOrder, List<Discrepancy>> {

    private final AuditService audit;

    public CrmSkipListener(AuditService audit) {
        this.audit = audit;
    }

    @Override
    public void onSkipInProcess(ErpOrder item, Throwable t) {
        audit.record(AuditAction.RECONCILE_SKIPPED, item.orderId(),
                "CRM unavailable after retries: " + t.getMessage());
    }
}
