package com.pfe.ekyc.service;

import com.pfe.ekyc.domain.AuditLog;
import com.pfe.ekyc.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Enregistre qui a fait quoi, quand (dans la transaction de l'appelant). */
    public void log(String actor, String action, Long caseId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setCaseId(caseId);
        entry.setDetails(details);
        repository.save(entry);
    }
}
