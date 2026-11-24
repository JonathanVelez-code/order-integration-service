package com.jonathanvelez.orderintegration.sync;

import static com.jonathanvelez.orderintegration.TestOrders.erpOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.external.OrderStatus;

/**
 * Transaction test: if the audit line can't be written, the order change must roll back
 * too. Otherwise the database would hold a change with no audit record of it.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderUpsertRollbackTest {

    @Autowired
    private OrderUpsertService upserts;

    @Autowired
    private SyncedOrderRepository orders;

    @MockBean
    private AuditService audit;

    @Test
    void orderIsNotSavedWhenTheAuditWriteFails() {
        orders.deleteAll();
        doThrow(new IllegalStateException("audit store down")).when(audit).record(any(), any(), any());

        assertThatThrownBy(() -> upserts.upsert(erpOrder("ORD-1", "100.00", OrderStatus.NEW, 1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orders.findById("ORD-1")).isEmpty();
    }
}
