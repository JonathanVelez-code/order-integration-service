package com.jonathanvelez.orderintegration.audit;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jonathanvelez.orderintegration.domain.AuditAction;
import com.jonathanvelez.orderintegration.domain.AuditEntry;
import com.jonathanvelez.orderintegration.domain.AuditEntryRepository;

/**
 * Writes the audit log. It joins the caller's transaction, so an audit line is saved
 * only if the change it describes is saved too.
 */
@Service
public class AuditService {

    private final AuditEntryRepository repository;
    private final Clock clock;

    public AuditService(AuditEntryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void record(AuditAction action, String orderId, String detail) {
        repository.save(new AuditEntry(clock.instant(), action, orderId, detail));
    }
}
