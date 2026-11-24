package com.jonathanvelez.orderintegration.sync;

import java.time.Instant;

/** What one ERP-to-CRM sync did. */
public record SyncResult(
        int changedInErp,
        int created,
        int updated,
        int unchanged,
        int pushedToCrm,
        int pendingInCrm,
        Instant watermark) {
}
