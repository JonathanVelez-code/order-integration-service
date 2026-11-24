package com.jonathanvelez.orderintegration.sync;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jonathanvelez.orderintegration.domain.SyncState;
import com.jonathanvelez.orderintegration.domain.SyncStateRepository;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.CrmUnavailableException;
import com.jonathanvelez.orderintegration.external.ErpClient;
import com.jonathanvelez.orderintegration.external.ErpOrder;

/**
 * Pulls changed orders from the ERP and pushes them to the CRM.
 *
 * <p>Design choices:
 * <ul>
 *   <li>Each order is saved in its own short transaction ({@link OrderUpsertService}), so one
 *       bad order doesn't roll back the others.</li>
 *   <li>The CRM call happens outside the database transaction, so a slow CRM never holds
 *       database locks.</li>
 *   <li>If the CRM is still down after the retries, the order is marked PENDING and the
 *       nightly reconciliation repairs it. The sync keeps going.</li>
 *   <li>The watermark is saved only at the end. If the sync crashes halfway, the next run
 *       starts from the old watermark and re-processes those orders. That's safe because
 *       saving and pushing an order are idempotent.</li>
 * </ul>
 */
@Service
public class OrderSyncService {

    static final String WATERMARK = "erp-orders";
    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    private final ErpClient erp;
    private final CrmGateway crm;
    private final OrderUpsertService upserts;
    private final SyncStateRepository syncStates;

    public OrderSyncService(ErpClient erp, CrmGateway crm, OrderUpsertService upserts, SyncStateRepository syncStates) {
        this.erp = erp;
        this.crm = crm;
        this.upserts = upserts;
        this.syncStates = syncStates;
    }

    /** Synchronized so the scheduler and the API can't run two syncs at once. */
    public synchronized SyncResult syncChangedOrders() {
        Instant since = syncStates.findById(WATERMARK).map(SyncState::getWatermark).orElse(Instant.EPOCH);
        List<ErpOrder> changed = erp.fetchOrdersUpdatedSince(since);

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int pushed = 0;
        int pending = 0;
        Instant newWatermark = since;

        for (ErpOrder order : changed) {
            ChangeType change = upserts.upsert(order);
            switch (change) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
            if (change != ChangeType.UNCHANGED) {
                if (pushToCrm(order)) {
                    pushed++;
                } else {
                    pending++;
                }
            }
            if (order.updatedAt().isAfter(newWatermark)) {
                newWatermark = order.updatedAt();
            }
        }

        saveWatermark(newWatermark);
        SyncResult result = new SyncResult(changed.size(), created, updated, unchanged, pushed, pending, newWatermark);
        log.info("ERP sync: {}", result);
        return result;
    }

    private boolean pushToCrm(ErpOrder order) {
        try {
            crm.push(CrmOrder.from(order));
            upserts.markCrmSynced(order.orderId());
            return true;
        } catch (CrmUnavailableException e) {
            upserts.markCrmPending(order.orderId(), "CRM push failed after retries: " + e.getMessage());
            return false;
        }
    }

    // No @Transactional here: Spring ignores it on a method called from inside the same class
    // (the call doesn't go through the proxy). The repository's save() is already transactional.
    private void saveWatermark(Instant watermark) {
        SyncState state = syncStates.findById(WATERMARK).orElseGet(() -> new SyncState(WATERMARK, watermark));
        state.setWatermark(watermark);
        syncStates.save(state);
    }
}
