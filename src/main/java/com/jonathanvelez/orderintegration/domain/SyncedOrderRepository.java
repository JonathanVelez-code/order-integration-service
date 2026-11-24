package com.jonathanvelez.orderintegration.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncedOrderRepository extends JpaRepository<SyncedOrder, String> {

    List<SyncedOrder> findByCrmSyncStatusOrderByOrderIdAsc(CrmSyncStatus status);

    long countByCrmSyncStatus(CrmSyncStatus status);
}
