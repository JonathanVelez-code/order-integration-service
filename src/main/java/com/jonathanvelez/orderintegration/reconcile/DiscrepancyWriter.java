package com.jonathanvelez.orderintegration.reconcile;

import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;

/**
 * Step 1, "write": saves each chunk of discrepancies and audits them. Spring Batch runs
 * each chunk in one transaction, so a chunk is saved completely or not at all.
 */
public class DiscrepancyWriter implements ItemWriter<List<Discrepancy>> {

    private final DiscrepancyRepository repository;
    private final AuditService audit;
    private final Long jobExecutionId;

    public DiscrepancyWriter(DiscrepancyRepository repository, AuditService audit, Long jobExecutionId) {
        this.repository = repository;
        this.audit = audit;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public void write(Chunk<? extends List<Discrepancy>> chunk) {
        for (List<Discrepancy> forOneOrder : chunk) {
            for (Discrepancy d : forOneOrder) {
                d.assignTo(jobExecutionId);
                repository.save(d);
                audit.record(AuditAction.DISCREPANCY_FOUND, d.getOrderId(),
                        "%s: ERP=%s, CRM=%s".formatted(d.getType(), d.getErpValue(), d.getCrmValue()));
            }
        }
    }
}
