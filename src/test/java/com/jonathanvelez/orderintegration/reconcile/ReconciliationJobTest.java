package com.jonathanvelez.orderintegration.reconcile;

import static com.jonathanvelez.orderintegration.TestOrders.erpOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jonathanvelez.orderintegration.IntegrationTestBase;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyType;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.external.OrderStatus;

/** Runs the real Spring Batch job end to end against the simulated ERP and CRM. */
class ReconciliationJobTest extends IntegrationTestBase {

    @Autowired
    private ReconciliationService reconciliation;

    @Test
    void findsEveryKindOfDriftAndRepairsTheCrm() throws Exception {
        ErpOrder inSync = erpOrder("ORD-1", "100.00", OrderStatus.PAID, 1);
        ErpOrder missing = erpOrder("ORD-2", "200.00", OrderStatus.NEW, 2);
        ErpOrder wrongAmount = erpOrder("ORD-3", "300.00", OrderStatus.PAID, 3);
        ErpOrder wrongStatus = erpOrder("ORD-4", "400.00", OrderStatus.SHIPPED, 4);
        ErpOrder wrongBoth = erpOrder("ORD-5", "500.00", OrderStatus.CANCELLED, 5);
        for (ErpOrder o : new ErpOrder[] {inSync, missing, wrongAmount, wrongStatus, wrongBoth}) {
            erp.put(o);
        }
        crm.put(CrmOrder.from(inSync));
        crm.put(new CrmOrder("ORD-3", "CUST-001", new BigDecimal("310.00"), OrderStatus.PAID));
        crm.put(new CrmOrder("ORD-4", "CUST-001", new BigDecimal("400.00"), OrderStatus.PAID));
        crm.put(new CrmOrder("ORD-5", "CUST-001", new BigDecimal("450.00"), OrderStatus.PAID));

        ReconciliationSummary summary = reconciliation.run();

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.ordersChecked()).isEqualTo(5);
        assertThat(summary.discrepanciesFound()).isEqualTo(5); // ORD-5 has two
        assertThat(summary.discrepanciesResolved()).isEqualTo(5);
        assertThat(discrepancies.findByJobExecutionIdOrderByIdAsc(summary.jobExecutionId()))
                .extracting(Discrepancy::getOrderId, Discrepancy::getType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("ORD-2", DiscrepancyType.MISSING_IN_CRM),
                        org.assertj.core.groups.Tuple.tuple("ORD-3", DiscrepancyType.AMOUNT_MISMATCH),
                        org.assertj.core.groups.Tuple.tuple("ORD-4", DiscrepancyType.STATUS_MISMATCH),
                        org.assertj.core.groups.Tuple.tuple("ORD-5", DiscrepancyType.AMOUNT_MISMATCH),
                        org.assertj.core.groups.Tuple.tuple("ORD-5", DiscrepancyType.STATUS_MISMATCH));

        // The CRM now matches the ERP for every order
        for (ErpOrder o : new ErpOrder[] {inSync, missing, wrongAmount, wrongStatus, wrongBoth}) {
            assertThat(crm.peek(o.orderId())).contains(CrmOrder.from(o));
        }
        assertThat(auditEntries.countByAction(AuditAction.DISCREPANCY_REPAIRED)).isEqualTo(4);
        assertThat(auditEntries.countByAction(AuditAction.RECONCILIATION_COMPLETED)).isEqualTo(1);
    }

    @Test
    void secondRunFindsNothingAfterTheRepair() throws Exception {
        erp.put(erpOrder("ORD-1", "100.00", OrderStatus.PAID, 1));

        assertThat(reconciliation.run().discrepanciesFound()).isEqualTo(1);
        ReconciliationSummary second = reconciliation.run();

        assertThat(second.status()).isEqualTo("COMPLETED");
        assertThat(second.discrepanciesFound()).isZero();
    }

    @Test
    void orderIsSkippedAndAuditedWhenCrmStaysDown() throws Exception {
        for (int i = 1; i <= 3; i++) {
            ErpOrder o = erpOrder("ORD-" + i, "100.00", OrderStatus.PAID, i);
            erp.put(o);
            crm.put(CrmOrder.from(o));
        }
        crm.makeUnreachable("ORD-2");

        ReconciliationSummary summary = reconciliation.run();

        assertThat(summary.status()).isEqualTo("COMPLETED"); // one bad order doesn't fail the run
        assertThat(summary.ordersSkipped()).isEqualTo(1);
        assertThat(auditEntries.findByOrderIdOrderByIdAsc("ORD-2"))
                .extracting(e -> e.getAction())
                .containsExactly(AuditAction.RECONCILE_SKIPPED);
    }

    @Test
    void temporaryCrmFailuresDuringTheCheckAreRetried() throws Exception {
        ErpOrder o = erpOrder("ORD-1", "100.00", OrderStatus.PAID, 1);
        erp.put(o);
        crm.put(CrmOrder.from(o));
        crm.failNextCalls(2);

        ReconciliationSummary summary = reconciliation.run();

        assertThat(summary.ordersSkipped()).isZero();
        assertThat(summary.discrepanciesFound()).isZero();
        assertThat(crm.callCount()).isEqualTo(3);
    }
}
