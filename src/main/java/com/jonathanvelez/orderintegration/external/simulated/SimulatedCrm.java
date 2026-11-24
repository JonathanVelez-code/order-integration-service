package com.jonathanvelez.orderintegration.external.simulated;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.external.CrmClient;
import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.CrmUnavailableException;

/**
 * An in-memory stand-in for the CRM. It can be told to fail, so the retry, skip and
 * pending paths can be demonstrated and tested.
 */
@Component
public class SimulatedCrm implements CrmClient {

    private final Map<String, CrmOrder> orders = new ConcurrentHashMap<>();
    private final Set<String> unreachableOrders = ConcurrentHashMap.newKeySet();
    private final AtomicInteger failuresToInject = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();
    private final double randomFailureRate;
    private final Random random = new Random(7);

    public SimulatedCrm(@Value("${integration.demo.crm-failure-rate:0.0}") double randomFailureRate) {
        this.randomFailureRate = randomFailureRate;
    }

    @Override
    public Optional<CrmOrder> findOrder(String orderId) {
        maybeFail(orderId);
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public void upsertOrder(CrmOrder order) {
        maybeFail(order.orderId());
        orders.put(order.orderId(), order);
    }

    // ----- helpers for the demo and the tests -----

    /** Writes directly, without failures, to set up a starting state. */
    public void put(CrmOrder order) {
        orders.put(order.orderId(), order);
    }

    public Optional<CrmOrder> peek(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    /** The next {@code n} calls fail, then calls succeed again. */
    public void failNextCalls(int n) {
        failuresToInject.set(n);
    }

    /** Every call about this order fails, as if the CRM always timed out on it. */
    public void makeUnreachable(String orderId) {
        unreachableOrders.add(orderId);
    }

    public int callCount() {
        return calls.get();
    }

    public void clear() {
        orders.clear();
        unreachableOrders.clear();
        failuresToInject.set(0);
        calls.set(0);
    }

    private void maybeFail(String orderId) {
        calls.incrementAndGet();
        if (unreachableOrders.contains(orderId)) {
            throw new CrmUnavailableException("CRM timed out on order " + orderId);
        }
        if (failuresToInject.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            throw new CrmUnavailableException("CRM temporarily unavailable");
        }
        if (randomFailureRate > 0) {
            boolean fail;
            synchronized (random) {
                fail = random.nextDouble() < randomFailureRate;
            }
            if (fail) {
                throw new CrmUnavailableException("CRM temporarily unavailable (random)");
            }
        }
    }
}
