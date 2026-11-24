package com.jonathanvelez.orderintegration.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findTop100ByOrderByIdDesc();

    List<AuditEntry> findByOrderIdOrderByIdAsc(String orderId);

    long countByAction(AuditAction action);
}
