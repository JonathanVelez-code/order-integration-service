package com.jonathanvelez.orderintegration.web;

import java.util.List;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jonathanvelez.orderintegration.domain.AuditEntry;
import com.jonathanvelez.orderintegration.domain.AuditEntryRepository;
import com.jonathanvelez.orderintegration.domain.CrmSyncStatus;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;
import com.jonathanvelez.orderintegration.domain.SyncedOrder;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.reconcile.ReconciliationService;
import com.jonathanvelez.orderintegration.reconcile.ReconciliationSummary;
import com.jonathanvelez.orderintegration.sync.OrderSyncService;
import com.jonathanvelez.orderintegration.sync.SyncResult;

/** REST API to run a sync or a reconciliation on demand and to inspect the results. */
@RestController
@RequestMapping("/api")
public class IntegrationController {

    private final OrderSyncService syncService;
    private final ReconciliationService reconciliationService;
    private final SyncedOrderRepository orders;
    private final DiscrepancyRepository discrepancies;
    private final AuditEntryRepository auditEntries;

    public IntegrationController(OrderSyncService syncService, ReconciliationService reconciliationService,
            SyncedOrderRepository orders, DiscrepancyRepository discrepancies, AuditEntryRepository auditEntries) {
        this.syncService = syncService;
        this.reconciliationService = reconciliationService;
        this.orders = orders;
        this.discrepancies = discrepancies;
        this.auditEntries = auditEntries;
    }

    /** Pull ERP changes now instead of waiting for the 15-minute schedule. */
    @PostMapping("/sync")
    public SyncResult sync() {
        return syncService.syncChangedOrders();
    }

    /** All synced orders, or only those whose CRM copy is PENDING. */
    @GetMapping("/orders")
    public List<SyncedOrder> orders(@RequestParam(required = false) CrmSyncStatus crmStatus) {
        return crmStatus == null ? orders.findAll() : orders.findByCrmSyncStatusOrderByOrderIdAsc(crmStatus);
    }

    /** Run the reconciliation job now instead of waiting for the nightly schedule. */
    @PostMapping("/reconciliations")
    public ReconciliationSummary reconcile() throws JobExecutionException {
        return reconciliationService.run();
    }

    @GetMapping("/reconciliations/{jobExecutionId}/discrepancies")
    public List<Discrepancy> discrepancies(@PathVariable Long jobExecutionId) {
        return discrepancies.findByJobExecutionIdOrderByIdAsc(jobExecutionId);
    }

    /** The latest 100 audit entries, or the full history of one order. */
    @GetMapping("/audit")
    public List<AuditEntry> audit(@RequestParam(required = false) String orderId) {
        return orderId == null ? auditEntries.findTop100ByOrderByIdDesc() : auditEntries.findByOrderIdOrderByIdAsc(orderId);
    }
}
