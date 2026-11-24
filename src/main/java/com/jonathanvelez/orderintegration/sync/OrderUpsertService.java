package com.jonathanvelez.orderintegration.sync;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.SyncedOrder;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.external.ErpOrder;

/**
 * Database writes for one order. Each method is one transaction: the order change and its
 * audit line are saved together, or neither is.
 */
@Service
public class OrderUpsertService {

    private final SyncedOrderRepository orders;
    private final AuditService audit;
    private final Clock clock;

    public OrderUpsertService(SyncedOrderRepository orders, AuditService audit, Clock clock) {
        this.orders = orders;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ChangeType upsert(ErpOrder erp) {
        Optional<SyncedOrder> existing = orders.findById(erp.orderId());
        if (existing.isEmpty()) {
            orders.save(new SyncedOrder(erp));
            audit.record(AuditAction.ORDER_CREATED, erp.orderId(), describe(erp));
            return ChangeType.CREATED;
        }
        SyncedOrder order = existing.get();
        if (order.matches(erp)) {
            return ChangeType.UNCHANGED;
        }
        order.applyFrom(erp);
        order.markPending();
        audit.record(AuditAction.ORDER_UPDATED, erp.orderId(), describe(erp));
        return ChangeType.UPDATED;
    }

    @Transactional
    public void markCrmSynced(String orderId) {
        orders.findById(orderId).ifPresent(o -> o.markSynced(clock.instant()));
        audit.record(AuditAction.CRM_PUSHED, orderId, "Pushed to CRM");
    }

    @Transactional
    public void markCrmPending(String orderId, String reason) {
        orders.findById(orderId).ifPresent(SyncedOrder::markPending);
        audit.record(AuditAction.CRM_PUSH_FAILED, orderId, reason);
    }

    static String describe(ErpOrder erp) {
        return "customer=%s amount=%s status=%s".formatted(erp.customerId(), erp.amount().toPlainString(), erp.status());
    }
}
