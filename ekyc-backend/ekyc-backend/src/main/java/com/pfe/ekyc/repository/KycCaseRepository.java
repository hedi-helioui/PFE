package com.pfe.ekyc.repository;

import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.domain.KycCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycCaseRepository extends JpaRepository<KycCase, Long> {

    List<KycCase> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<KycCase> findByStatus(CaseStatus status, Pageable pageable);
}
