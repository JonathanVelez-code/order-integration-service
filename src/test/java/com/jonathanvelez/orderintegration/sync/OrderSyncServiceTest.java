package com.jonathanvelez.orderintegration.sync;

import static com.jonathanvelez.orderintegration.TestOrders.erpOrder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jonathanvelez.orderintegration.IntegrationTestBase;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.CrmSyncStatus;
import com.jonathanvelez.orderintegration.external.OrderStatus;

class OrderSyncServiceTest extends IntegrationTestBase {

    @Autowired
    private OrderSyncService syncService;

    @Test
    void newErpOrdersAreSavedAndPushedToCrm() {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1));
        erp.put(erpOrder("ORD-2", "250.50", OrderStatus.PAID, 2));
        erp.put(erpOrder("ORD-3", "75.25", OrderStatus.SHIPPED, 3));

        SyncResult result = syncService.syncChangedOrders();

        assertThat(result.created()).isEqualTo(3);
        assertThat(result.pushedToCrm()).isEqualTo(3);
        assertThat(orders.countByCrmSyncStatus(CrmSyncStatus.SYNCED)).isEqualTo(3);
        assertThat(crm.peek("ORD-2")).hasValueSatisfying(o -> assertThat(o.amount()).isEqualByComparingTo("250.50"));
        assertThat(auditEntries.countByAction(AuditAction.ORDER_CREATED)).isEqualTo(3);
        assertThat(auditEntries.countByAction(AuditAction.CRM_PUSHED)).isEqualTo(3);
    }

    @Test
    void nextSyncOnlyPicksUpOrdersChangedSinceTheWatermark() {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1));
        erp.put(erpOrder("ORD-2", "200.00", OrderStatus.NEW, 2));
        syncService.syncChangedOrders();

        erp.put(erpOrder("ORD-2", "200.00", OrderStatus.PAID, 10)); // ORD-2 was paid later
        SyncResult second = syncService.syncChangedOrders();

        assertThat(second.changedInErp()).isEqualTo(1);
        assertThat(second.updated()).isEqualTo(1);
        assertThat(crm.peek("ORD-2")).hasValueSatisfying(o -> assertThat(o.status()).isEqualTo(OrderStatus.PAID));
        assertThat(syncService.syncChangedOrders().changedInErp()).isZero();
    }

    @Test
    void unchangedOrderIsNotPushedAgain() {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1));
        syncService.syncChangedOrders();
        int callsAfterFirstSync = crm.callCount();

        // Same data with a newer timestamp, e.g. the ERP touched a field the CRM doesn't store
        erp.put(erpOrder("ORD-1", "100.0", OrderStatus.NEW, 5));
        SyncResult second = syncService.syncChangedOrders();

        assertThat(second.unchanged()).isEqualTo(1);
        assertThat(crm.callCount()).isEqualTo(callsAfterFirstSync);
    }

    @Test
    void temporaryCrmFailuresAreRetried() {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1));
        crm.failNextCalls(2); // fails twice, then works

        SyncResult result = syncService.syncChangedOrders();

        assertThat(result.pushedToCrm()).isEqualTo(1);
        assertThat(crm.callCount()).isEqualTo(3); // 1 call + 2 retries
        assertThat(orders.findById("ORD-1")).hasValueSatisfying(o ->
                assertThat(o.getCrmSyncStatus()).isEqualTo(CrmSyncStatus.SYNCED));
    }

    @Test
    void orderIsMarkedPendingWhenCrmStaysDown() {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1));
        erp.put(erpOrder("ORD-2", "200.00", OrderStatus.NEW, 2));
        crm.makeUnreachable("ORD-1");

        SyncResult result = syncService.syncChangedOrders();

        assertThat(result.pendingInCrm()).isEqualTo(1);
        assertThat(result.pushedToCrm()).isEqualTo(1); // one bad order doesn't stop the others
        assertThat(orders.findById("ORD-1")).hasValueSatisfying(o ->
                assertThat(o.getCrmSyncStatus()).isEqualTo(CrmSyncStatus.PENDING));
        assertThat(auditEntries.findByOrderIdOrderByIdAsc("ORD-1"))
                .extracting(e -> e.getAction())
                .containsExactly(AuditAction.ORDER_CREATED, AuditAction.CRM_PUSH_FAILED);
        assertThat(crm.peek("ORD-1")).isEmpty();
    }
}
