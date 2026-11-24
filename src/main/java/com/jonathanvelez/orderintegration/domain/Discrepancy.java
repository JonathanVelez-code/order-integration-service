package com.jonathanvelez.orderintegration.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** A difference between the ERP and the CRM, found by one reconciliation run. */
@Entity
@Table(name = "discrepancy", indexes = @Index(name = "idx_discrepancy_run", columnList = "jobExecutionId"))
public class Discrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Spring Batch job execution that found it. */
    private Long jobExecutionId;

    @Column(nullable = false, length = 40)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscrepancyType type;

    @Column(length = 200)
    private String erpValue;

    @Column(length = 200)
    private String crmValue;

    @Column(nullable = false)
    private Instant detectedAt;

    private boolean resolved;

    private Instant resolvedAt;

    protected Discrepancy() {
        // for JPA
    }

    public Discrepancy(String orderId, DiscrepancyType type, String erpValue, String crmValue, Instant detectedAt) {
        this.orderId = orderId;
        this.type = type;
        this.erpValue = erpValue;
        this.crmValue = crmValue;
        this.detectedAt = detectedAt;
    }

    public void assignTo(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public void markResolved(Instant at) {
        this.resolved = true;
        this.resolvedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public DiscrepancyType getType() {
        return type;
    }

    public String getErpValue() {
        return erpValue;
    }

    public String getCrmValue() {
        return crmValue;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
