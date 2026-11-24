package com.jonathanvelez.orderintegration.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.jonathanvelez.orderintegration.external.ErpOrder;
import com.jonathanvelez.orderintegration.external.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** This service's copy of an ERP order, plus whether the CRM is up to date. */
@Entity
@Table(name = "synced_order")
public class SyncedOrder {

    @Id
    @Column(length = 40)
    private String orderId;

    @Column(nullable = false, length = 40)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant erpUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrmSyncStatus crmSyncStatus;

    private Instant crmSyncedAt;

    /** Optimistic locking: two writers can't silently overwrite each other. */
    @Version
    private long version;

    protected SyncedOrder() {
        // for JPA
    }

    public SyncedOrder(ErpOrder erp) {
        this.orderId = erp.orderId();
        applyFrom(erp);
        this.crmSyncStatus = CrmSyncStatus.PENDING;
    }

    /** True if nothing the CRM cares about has changed. Amounts use compareTo, so 10.0 equals 10.00. */
    public boolean matches(ErpOrder erp) {
        return customerId.equals(erp.customerId())
                && amount.compareTo(erp.amount()) == 0
                && status == erp.status();
    }

    public void applyFrom(ErpOrder erp) {
        this.customerId = erp.customerId();
        this.amount = erp.amount();
        this.status = erp.status();
        this.erpUpdatedAt = erp.updatedAt();
    }

    public void markSynced(Instant at) {
        this.crmSyncStatus = CrmSyncStatus.SYNCED;
        this.crmSyncedAt = at;
    }

    public void markPending() {
        this.crmSyncStatus = CrmSyncStatus.PENDING;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getErpUpdatedAt() {
        return erpUpdatedAt;
    }

    public CrmSyncStatus getCrmSyncStatus() {
        return crmSyncStatus;
    }

    public Instant getCrmSyncedAt() {
        return crmSyncedAt;
    }

    public long getVersion() {
        return version;
    }
}
