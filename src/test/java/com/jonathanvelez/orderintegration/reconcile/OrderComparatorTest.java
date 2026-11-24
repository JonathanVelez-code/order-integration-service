package com.jonathanvelez.orderintegration.reconcile;

import static com.jonathanvelez.orderintegration.TestOrders.T0;
import static com.jonathanvelez.orderintegration.TestOrders.erpOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyType;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.external.OrderStatus;

/** Plain unit tests: no Spring, no database. */
class OrderComparatorTest {

    private final ErpOrder erp = erpOrder("ORD-1", "100.00", OrderStatus.PAID, 0);

    @Test
    void identicalOrdersHaveNoDiscrepancies() {
        assertThat(OrderComparator.compare(erp, Optional.of(CrmOrder.from(erp)), T0)).isEmpty();
    }

    @Test
    void orderMissingFromCrmIsReported() {
        List<Discrepancy> found = OrderComparator.compare(erp, Optional.empty(), T0);

        assertThat(found).extracting(Discrepancy::getType).containsExactly(DiscrepancyType.MISSING_IN_CRM);
    }

    @Test
    void amountsWithDifferentScaleAreEqual() {
        // BigDecimal.equals("100.0", "100.00") is false; the comparator must use compareTo
        CrmOrder crm = new CrmOrder("ORD-1", "CUST-001", new BigDecimal("100.0"), OrderStatus.PAID);

        assertThat(OrderComparator.compare(erp, Optional.of(crm), T0)).isEmpty();
    }

    @Test
    void eachDifferentFieldIsReportedSeparately() {
        CrmOrder crm = new CrmOrder("ORD-1", "CUST-999", new BigDecimal("90.00"), OrderStatus.NEW);

        List<Discrepancy> found = OrderComparator.compare(erp, Optional.of(crm), T0);

        assertThat(found).extracting(Discrepancy::getType).containsExactly(
                DiscrepancyType.CUSTOMER_MISMATCH, DiscrepancyType.AMOUNT_MISMATCH, DiscrepancyType.STATUS_MISMATCH);
        assertThat(found.get(1).getErpValue()).isEqualTo("100.00");
        assertThat(found.get(1).getCrmValue()).isEqualTo("90.00");
    }
}
