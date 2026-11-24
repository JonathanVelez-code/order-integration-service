package com.jonathanvelez.orderintegration;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Keeps orders consistent between an ERP (the system of record) and a CRM.
 *
 * <ul>
 *   <li>Every 15 minutes, {@code OrderSyncService} pulls changed ERP orders, saves them
 *       in a transaction, and pushes them to the CRM with retries.</li>
 *   <li>Every night, the {@code reconciliationJob} (Spring Batch) compares every ERP order
 *       with the CRM, records each discrepancy, and repairs the CRM.</li>
 *   <li>Every change is written to an audit log.</li>
 * </ul>
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class OrderIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderIntegrationApplication.class, args);
    }

    /** One clock for the whole app, so tests can control time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
