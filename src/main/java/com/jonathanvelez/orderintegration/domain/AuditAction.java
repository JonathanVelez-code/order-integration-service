package com.jonathanvelez.orderintegration.domain;

public enum AuditAction {
    ORDER_CREATED,
    ORDER_UPDATED,
    CRM_PUSHED,
    CRM_PUSH_FAILED,
    DISCREPANCY_FOUND,
    RECONCILE_SKIPPED,
    DISCREPANCY_REPAIRED,
    REPAIR_FAILED,
    RECONCILIATION_COMPLETED
}
