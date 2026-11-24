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

/** One line in the audit log: what happened, to which order, and when. Never updated. */
@Entity
@Table(name = "audit_entry", indexes = @Index(name = "idx_audit_order", columnList = "orderId"))
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(length = 40)
    private String orderId;

    @Column(length = 500)
    private String detail;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(Instant occurredAt, AuditAction action, String orderId, String detail) {
        this.occurredAt = occurredAt;
        this.action = action;
        this.orderId = orderId;
        this.detail = detail == null || detail.length() <= 500 ? detail : detail.substring(0, 500);
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDetail() {
        return detail;
    }
}
