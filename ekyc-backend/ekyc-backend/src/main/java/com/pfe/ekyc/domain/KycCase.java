package com.pfe.ekyc.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kyc_cases")
public class KycCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.PENDING;

    @Column(name = "declared_full_name", nullable = false)
    private String declaredFullName;

    @Column(name = "declared_id_number", nullable = false)
    private String declaredIdNumber;

    @Column(name = "declared_birth_date", nullable = false)
    private LocalDate declaredBirthDate;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "kycCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KycDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "kycCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KycMedia> media = new ArrayList<>();

    @OneToOne(mappedBy = "kycCase", cascade = CascadeType.ALL)
    private VerificationResult result;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public String getDeclaredFullName() { return declaredFullName; }
    public void setDeclaredFullName(String declaredFullName) { this.declaredFullName = declaredFullName; }
    public String getDeclaredIdNumber() { return declaredIdNumber; }
    public void setDeclaredIdNumber(String declaredIdNumber) { this.declaredIdNumber = declaredIdNumber; }
    public LocalDate getDeclaredBirthDate() { return declaredBirthDate; }
    public void setDeclaredBirthDate(LocalDate declaredBirthDate) { this.declaredBirthDate = declaredBirthDate; }
    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<KycDocument> getDocuments() { return documents; }
    public List<KycMedia> getMedia() { return media; }
    public VerificationResult getResult() { return result; }
    public void setResult(VerificationResult result) { this.result = result; }
}
