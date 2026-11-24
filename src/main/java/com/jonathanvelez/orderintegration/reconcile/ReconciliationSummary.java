package com.jonathanvelez.orderintegration.reconcile;

/** What one reconciliation run found and fixed. */
public record ReconciliationSummary(
        Long jobExecutionId,
        String status,
        long ordersChecked,
        long discrepanciesFound,
        long discrepanciesResolved,
        long ordersSkipped) {
}
