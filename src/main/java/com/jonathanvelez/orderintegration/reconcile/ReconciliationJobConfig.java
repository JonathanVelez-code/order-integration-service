package com.jonathanvelez.orderintegration.reconcile;

import java.time.Clock;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.jonathanvelez.orderintegration.audit.AuditService;
import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;
import com.jonathanvelez.orderintegration.domain.SyncedOrderRepository;
import com.jonathanvelez.orderintegration.external.CrmUnavailableException;
import com.jonathanvelez.orderintegration.external.ErpClient;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.sync.CrmGateway;

/**
 * The nightly reconciliation job.
 *
 * <pre>
 * findDiscrepancies (chunks of 50):  read every ERP order -> compare with the CRM -> save differences
 * repairCrm (tasklet):               push the ERP version for each order with a difference
 * </pre>
 *
 * Beans marked {@code @StepScope} are created fresh for each run, so they can read the run's
 * job execution ID.
 */
@Configuration
public class ReconciliationJobConfig {

    static final int CHUNK_SIZE = 50;
    static final int MAX_SKIPS = 100;

    @Bean
    Job reconciliationJob(JobRepository jobRepository, Step findDiscrepanciesStep, Step repairCrmStep,
            ReconciliationJobListener listener) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .listener(listener)
                .start(findDiscrepanciesStep)
                .next(repairCrmStep)
                .build();
    }

    @Bean
    Step findDiscrepanciesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ListItemReader<ErpOrder> erpOrderReader, DiscrepancyProcessor discrepancyProcessor,
            DiscrepancyWriter discrepancyWriter, AuditService audit) {
        return new StepBuilder("findDiscrepancies", jobRepository)
                .<ErpOrder, List<Discrepancy>>chunk(CHUNK_SIZE, transactionManager)
                .reader(erpOrderReader)
                .processor(discrepancyProcessor)
                .writer(discrepancyWriter)
                .faultTolerant()
                .skip(CrmUnavailableException.class)
                .skipLimit(MAX_SKIPS)
                .listener(new CrmSkipListener(audit))
                .build();
    }

    @Bean
    Step repairCrmStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RepairTasklet repairTasklet) {
        return new StepBuilder("repairCrm", jobRepository)
                .tasklet(repairTasklet, transactionManager)
                .build();
    }

    /** Reads the ERP when the step starts, not when the app starts. */
    @Bean
    @StepScope
    ListItemReader<ErpOrder> erpOrderReader(ErpClient erp) {
        return new ListItemReader<>(erp.fetchAllOrders());
    }

    @Bean
    DiscrepancyProcessor discrepancyProcessor(CrmGateway crm, Clock clock) {
        return new DiscrepancyProcessor(crm, clock);
    }

    @Bean
    @StepScope
    DiscrepancyWriter discrepancyWriter(DiscrepancyRepository repository, AuditService audit,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new DiscrepancyWriter(repository, audit, jobExecutionId);
    }

    @Bean
    @StepScope
    RepairTasklet repairTasklet(DiscrepancyRepository discrepancies, SyncedOrderRepository orders, ErpClient erp,
            CrmGateway crm, AuditService audit, Clock clock,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new RepairTasklet(discrepancies, orders, erp, crm, audit, clock, jobExecutionId);
    }
}
