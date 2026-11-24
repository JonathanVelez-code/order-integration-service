package com.jonathanvelez.orderintegration.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jonathanvelez.orderintegration.external.CrmOrder;
import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.external.OrderStatus;
import com.jonathanvelez.orderintegration.external.simulated.SimulatedCrm;
import com.jonathanvelez.orderintegration.external.simulated.SimulatedErp;

/**
 * Loads 200 sample orders into the simulated ERP, and a CRM copy that has drifted:
 * about 5% of orders are missing, and about 6% have a stale amount or status.
 * Turned off in tests ({@code integration.demo.seed=false}).
 */
@Component
@ConditionalOnProperty(name = "integration.demo.seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final int ORDERS = 200;

    private final SimulatedErp erp;
    private final SimulatedCrm crm;
    private final Clock clock;

    public DemoDataSeeder(SimulatedErp erp, SimulatedCrm crm, Clock clock) {
        this.erp = erp;
        this.crm = crm;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Random random = new Random(42);
        Instant start = clock.instant().minus(30, ChronoUnit.DAYS);
        int missing = 0;
        int stale = 0;
        for (int i = 1; i <= ORDERS; i++) {
            String orderId = "ORD-%05d".formatted(i);
            String customerId = "CUST-%03d".formatted(1 + random.nextInt(40));
            BigDecimal amount = BigDecimal.valueOf(50 + random.nextInt(5000)).add(BigDecimal.valueOf(random.nextInt(100), 2));
            OrderStatus status = OrderStatus.values()[random.nextInt(OrderStatus.values().length)];
            ErpOrder order = new ErpOrder(orderId, customerId, amount, status, start.plus(i, ChronoUnit.HOURS));
            erp.put(order);

            double roll = random.nextDouble();
            if (roll < 0.05) {
                missing++;                       // never reached the CRM
            } else if (roll < 0.08) {
                stale++;                         // CRM has an old amount
                crm.put(new CrmOrder(orderId, customerId, amount.add(new BigDecimal("10.00")), status));
            } else if (roll < 0.11 && status != OrderStatus.NEW) {
                stale++;                         // CRM missed a status change
                crm.put(new CrmOrder(orderId, customerId, amount, OrderStatus.NEW));
            } else {
                crm.put(CrmOrder.from(order));
            }
        }
        log.info("Demo data: {} ERP orders; CRM copy has {} missing and {} stale orders", ORDERS, missing, stale);
    }
}
