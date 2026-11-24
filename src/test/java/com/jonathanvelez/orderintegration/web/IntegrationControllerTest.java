package com.jonathanvelez.orderintegration.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jonathanvelez.orderintegration.domain.AuditEntryRepository;
import com.jonathanvelez.orderintegration.domain.CrmSyncStatus;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.reconcile.ReconciliationService;
import com.jonathanvelez.orderintegration.reconcile.ReconciliationSummary;
import com.jonathanvelez.orderintegration.sync.OrderSyncService;
import com.jonathanvelez.orderintegration.sync.SyncResult;

/** Web-layer slice test: only the controller is real, everything behind it is mocked. */
@WebMvcTest(IntegrationController.class)
class IntegrationControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean private OrderSyncService syncService;
    @MockBean private ReconciliationService reconciliationService;
    @MockBean private SyncedOrderRepository orders;
    @MockBean private DiscrepancyRepository discrepancies;
    @MockBean private AuditEntryRepository auditEntries;

    @Test
    void postSyncReturnsTheResult() throws Exception {
        when(syncService.syncChangedOrders())
                .thenReturn(new SyncResult(3, 2, 1, 0, 2, 1, Instant.parse("2026-11-01T00:03:00Z")));

        mvc.perform(post("/api/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.pendingInCrm").value(1))
                .andExpect(jsonPath("$.watermark").value("2026-11-01T00:03:00Z"));
    }

    @Test
    void postReconciliationReturnsTheSummary() throws Exception {
        when(reconciliationService.run()).thenReturn(new ReconciliationSummary(7L, "COMPLETED", 200, 12, 12, 0));

        mvc.perform(post("/api/reconciliations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(7))
                .andExpect(jsonPath("$.discrepanciesFound").value(12));
    }

    @Test
    void ordersCanBeFilteredByCrmStatus() throws Exception {
        when(orders.findByCrmSyncStatusOrderByOrderIdAsc(CrmSyncStatus.PENDING)).thenReturn(List.of());

        mvc.perform(get("/api/orders").param("crmStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void unknownCrmStatusIsABadRequest() throws Exception {
        mvc.perform(get("/api/orders").param("crmStatus", "LOST"))
                .andExpect(status().isBadRequest());
    }
}
