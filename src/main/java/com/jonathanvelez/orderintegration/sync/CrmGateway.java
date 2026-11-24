package com.jonathanvelez.orderintegration.sync;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.external.CrmClient;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.CrmUnavailableException;

/**
 * The only way the app talks to the CRM. Temporary failures are retried with exponential
 * backoff (Spring Retry, which wraps this bean in an AOP proxy). If every attempt fails,
 * the {@link CrmUnavailableException} reaches the caller, which decides what to do.
 */
@Component
public class CrmGateway {

    private static final Logger log = LoggerFactory.getLogger(CrmGateway.class);

    private final CrmClient crm;

    public CrmGateway(CrmClient crm) {
        this.crm = crm;
    }

    @Retryable(
            retryFor = CrmUnavailableException.class,
            maxAttemptsExpression = "${integration.crm.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${integration.crm.backoff-ms:200}", multiplier = 2))
    public void push(CrmOrder order) {
        try {
            crm.upsertOrder(order);
        } catch (CrmUnavailableException e) {
            log.warn("CRM push failed for {}: {}", order.orderId(), e.getMessage());
            throw e;
        }
    }

    @Retryable(
            retryFor = CrmUnavailableException.class,
            maxAttemptsExpression = "${integration.crm.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${integration.crm.backoff-ms:200}", multiplier = 2))
    public Optional<CrmOrder> find(String orderId) {
        return crm.findOrder(orderId);
    }
}
