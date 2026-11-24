package com.jonathanvelez.orderintegration.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {

    List<Discrepancy> findByJobExecutionIdOrderByIdAsc(Long jobExecutionId);

    List<Discrepancy> findByJobExecutionIdAndResolvedFalseOrderByIdAsc(Long jobExecutionId);
}
