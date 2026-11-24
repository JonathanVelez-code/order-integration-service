package com.jonathanvelez.orderintegration.domain;

public enum CrmSyncStatus {
    /** The CRM has the latest version of the order. */
    SYNCED,
    /** The order changed but the CRM push failed; the nightly reconciliation will fix it. */
    PENDING
}
