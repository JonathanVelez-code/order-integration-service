package com.jonathanvelez.orderintegration.reconcile;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.CrmUnavailableException;
import com.jonathanvelez.orderintegration.external.ErpClient;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.sync.CrmGateway;

/**
 * Step 2: repairs every order with an open discrepancy from this run by pushing the ERP
 * version to the CRM (the ERP is the system of record). Failures are audited and left open.
 */
public class RepairTasklet implements Tasklet {

    private final DiscrepancyRepository discrepancies;
    private final SyncedOrderRepository orders;
    private final ErpClient erp;
    private final CrmGateway crm;
    private final AuditService audit;
    private final Clock clock;
    private final Long jobExecutionId;

    public RepairTasklet(DiscrepancyRepository discrepancies, SyncedOrderRepository orders, ErpClient erp,
            CrmGateway crm, AuditService audit, Clock clock, Long jobExecutionId) {
        this.discrepancies = discrepancies;
        this.orders = orders;
        this.erp = erp;
        this.crm = crm;
        this.audit = audit;
        this.clock = clock;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Map<String, List<Discrepancy>> byOrder = discrepancies
                .findByJobExecutionIdAndResolvedFalseOrderByIdAsc(jobExecutionId).stream()
                .collect(Collectors.groupingBy(Discrepancy::getOrderId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Discrepancy>> entry : byOrder.entrySet()) {
            String orderId = entry.getKey();
            Optional<ErpOrder> erpOrder = erp.findOrder(orderId);
            if (erpOrder.isEmpty()) {
                continue;
            }
            try {
                crm.push(CrmOrder.from(erpOrder.get()));
                Instant now = clock.instant();
                entry.getValue().forEach(d -> d.markResolved(now));
                orders.findById(orderId).ifPresent(o -> o.markSynced(now));
                audit.record(AuditAction.DISCREPANCY_REPAIRED, orderId, "CRM updated from ERP: "
                        + entry.getValue().stream().map(d -> d.getType().name()).collect(Collectors.joining(", ")));
                contribution.incrementWriteCount(1);
            } catch (CrmUnavailableException e) {
                audit.record(AuditAction.REPAIR_FAILED, orderId, e.getMessage());
            }
        }
        return RepeatStatus.FINISHED;
    }
}
