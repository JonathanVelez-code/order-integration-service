package com.jonathanvelez.orderintegration.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, String> {
}
