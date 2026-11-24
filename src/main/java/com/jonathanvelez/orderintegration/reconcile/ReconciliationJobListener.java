package com.jonathanvelez.orderintegration.reconcile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.AuditAction;

/** Logs and audits a summary when each reconciliation run ends. */
@Component
public class ReconciliationJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJobListener.class);

    private final AuditService audit;

    public ReconciliationJobListener(AuditService audit) {
        this.audit = audit;
    }

    @Override
    public void afterJob(JobExecution execution) {
        long read = 0;
        long withDifferences = 0;
        long skipped = 0;
        long repaired = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            if (step.getStepName().equals("findDiscrepancies")) {
                read = step.getReadCount();
                withDifferences = step.getWriteCount();
                skipped = step.getSkipCount();
            } else if (step.getStepName().equals("repairCrm")) {
                repaired = step.getWriteCount();
            }
        }
        String summary = "run %d %s: %d orders checked, %d with differences, %d repaired, %d skipped"
                .formatted(execution.getId(), execution.getStatus(), read, withDifferences, repaired, skipped);
        log.info("Reconciliation {}", summary);
        audit.record(AuditAction.RECONCILIATION_COMPLETED, null, summary);
    }
}
