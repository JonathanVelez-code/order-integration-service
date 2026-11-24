package com.jonathanvelez.orderintegration.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.reconcile.ReconciliationService;
import com.jonathanvelez.orderintegration.sync.OrderSyncService;

/** Runs the ERP sync every 15 minutes and the reconciliation job every night (both configurable). */
@Component
@ConditionalOnProperty(name = "integration.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationScheduler.class);

    private final OrderSyncService syncService;
    private final ReconciliationService reconciliationService;

    public IntegrationScheduler(OrderSyncService syncService, ReconciliationService reconciliationService) {
        this.syncService = syncService;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "${integration.sync.cron}")
    public void syncFromErp() {
        syncService.syncChangedOrders();
    }

    @Scheduled(cron = "${integration.reconciliation.cron}", zone = "${integration.reconciliation.zone:UTC}")
    public void nightlyReconciliation() {
        try {
            log.info("Nightly reconciliation finished: {}", reconciliationService.run());
        } catch (JobExecutionException e) {
            log.error("Nightly reconciliation could not start", e);
        }
    }
}
