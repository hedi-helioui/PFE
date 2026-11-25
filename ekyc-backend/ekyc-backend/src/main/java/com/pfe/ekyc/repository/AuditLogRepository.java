package com.pfe.ekyc.repository;

import com.pfe.ekyc.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
