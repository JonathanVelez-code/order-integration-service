package com.jonathanvelez.orderintegration.reconcile;

import java.time.Clock;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.sync.CrmGateway;

/**
 * Step 1, "process": looks up each ERP order in the CRM (with retries) and returns its
 * discrepancies. Returning null tells Spring Batch to filter the item out (nothing to fix).
 */
public class DiscrepancyProcessor implements ItemProcessor<ErpOrder, List<Discrepancy>> {

    private final CrmGateway crm;
    private final Clock clock;

    public DiscrepancyProcessor(CrmGateway crm, Clock clock) {
        this.crm = crm;
        this.clock = clock;
    }

    @Override
    public List<Discrepancy> process(ErpOrder erpOrder) {
        List<Discrepancy> found = OrderComparator.compare(erpOrder, crm.find(erpOrder.orderId()), clock.instant());
        return found.isEmpty() ? null : found;
    }
}
