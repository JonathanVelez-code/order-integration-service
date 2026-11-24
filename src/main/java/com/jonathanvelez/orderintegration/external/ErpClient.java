package com.jonathanvelez.orderintegration.external;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read access to the ERP. A real deployment would implement this with a REST or JDBC client. */
public interface ErpClient {

    /** Orders created or changed after {@code since}, oldest first. */
    List<ErpOrder> fetchOrdersUpdatedSince(Instant since);

    /** Every order, sorted by ID. Used by the nightly reconciliation. */
    List<ErpOrder> fetchAllOrders();

    Optional<ErpOrder> findOrder(String orderId);
}
