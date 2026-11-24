package com.jonathanvelez.orderintegration.reconcile;

import java.util.List;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import com.jonathanvelez.orderintegration.domain.Discrepancy;
import com.jonathanvelez.orderintegration.domain.DiscrepancyRepository;

/** Starts a reconciliation run (from the nightly schedule or the API) and summarizes it. */
@Service
public class ReconciliationService {

    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;
    private final DiscrepancyRepository discrepancies;

    public ReconciliationService(JobLauncher jobLauncher, Job reconciliationJob, DiscrepancyRepository discrepancies) {
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
        this.discrepancies = discrepancies;
    }

    public synchronized ReconciliationSummary run() throws JobExecutionException {
        // A unique parameter makes every run a new job instance
        JobParameters parameters = new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(reconciliationJob, parameters);

        List<Discrepancy> found = discrepancies.findByJobExecutionIdOrderByIdAsc(execution.getId());
        long resolved = found.stream().filter(Discrepancy::isResolved).count();
        long checked = 0;
        long skipped = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            if (step.getStepName().equals("findDiscrepancies")) {
                checked = step.getReadCount();
                skipped = step.getSkipCount();
            }
        }
        return new ReconciliationSummary(execution.getId(), execution.getStatus().name(), checked, found.size(),
                resolved, skipped);
    }
}
