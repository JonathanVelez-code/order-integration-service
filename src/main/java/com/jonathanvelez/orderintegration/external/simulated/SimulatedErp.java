package com.jonathanvelez.orderintegration.external.simulated;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.external.ErpClient;
import com.jonathanvelez.orderintegration.external.ErpOrder;

/** An in-memory stand-in for the ERP, so the service runs without a real ERP. */
@Component
public class SimulatedErp implements ErpClient {

    private final Map<String, ErpOrder> orders = new ConcurrentHashMap<>();

    public void put(ErpOrder order) {
        orders.put(order.orderId(), order);
    }

    public void clear() {
        orders.clear();
    }

    @Override
    public List<ErpOrder> fetchOrdersUpdatedSince(Instant since) {
        return orders.values().stream()
                .filter(o -> o.updatedAt().isAfter(since))
                .sorted(Comparator.comparing(ErpOrder::updatedAt))
                .toList();
    }

    @Override
    public List<ErpOrder> fetchAllOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(ErpOrder::orderId))
                .toList();
    }

    @Override
    public Optional<ErpOrder> findOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
