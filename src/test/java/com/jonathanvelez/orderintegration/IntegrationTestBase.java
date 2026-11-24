package com.jonathanvelez.orderintegration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jonathanvelez.orderintegration.domain.AuditEntryRepository;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;
import com.jonathanvelez.orderintegration.domain.SyncStateRepository;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.external.simulated.SimulatedCrm;
import com.jonathanvelez.orderintegration.external.simulated.SimulatedErp;

/** Starts the whole app against an in-memory H2 database, and resets all data before each test. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired protected SimulatedErp erp;
    @Autowired protected SimulatedCrm crm;
    @Autowired protected SyncedOrderRepository orders;
    @Autowired protected AuditEntryRepository auditEntries;
    @Autowired protected DiscrepancyRepository discrepancies;
    @Autowired protected SyncStateRepository syncStates;

    @BeforeEach
    void resetData() {
        erp.clear();
        crm.clear();
        auditEntries.deleteAll();
        discrepancies.deleteAll();
        orders.deleteAll();
        syncStates.deleteAll();
    }
}
