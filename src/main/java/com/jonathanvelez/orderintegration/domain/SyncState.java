package com.jonathanvelez.orderintegration.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The sync watermark: the newest ERP change already processed. The next sync only asks
 * the ERP for changes after it.
 */
@Entity
@Table(name = "sync_state")
public class SyncState {

    @Id
    @Column(length = 40)
    private String name;

    @Column(nullable = false)
    private Instant watermark;

    protected SyncState() {
        // for JPA
    }

    public SyncState(String name, Instant watermark) {
        this.name = name;
        this.watermark = watermark;
    }

    public String getName() {
        return name;
    }

    public Instant getWatermark() {
        return watermark;
    }

    public void setWatermark(Instant watermark) {
        this.watermark = watermark;
    }
}
