package com.jonathanvelez.orderintegration.external;

import java.util.Optional;

/**
 * Read and write access to the CRM.
 *
 * <p>Calls can fail with {@link CrmUnavailableException} (timeouts, outages). Callers go
 * through {@code CrmGateway}, which retries those failures.
 */
public interface CrmClient {

    Optional<CrmOrder> findOrder(String orderId);

    /** Creates the order, or replaces it if it already exists. Safe to repeat. */
    void upsertOrder(CrmOrder order);
}
