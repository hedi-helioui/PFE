package com.pfe.ekyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Qui : email de l'utilisateur ou "system". */
    @Column(nullable = false)
    private String actor;

    /** Quoi : CASE_CREATED, CASE_SUBMITTED, MANUAL_DECISION, etc. */
    @Column(name = "action_type", nullable = false)
    private String action;

    @Column(name = "case_id")
    private Long caseId;

    @Column(length = 1000)
    private String details;

    /** Quand. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Instant getCreatedAt() { return createdAt; }
}
