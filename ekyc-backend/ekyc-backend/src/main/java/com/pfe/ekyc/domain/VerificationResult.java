package com.pfe.ekyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "verification_results")
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private KycCase kycCase;

    /** Similarité visage selfie / document, 0..1 (plus haut = meilleur). */
    @Column(name = "face_match_score", nullable = false)
    private double faceMatchScore;

    /** Score de vivacité, 0..1 (plus haut = meilleur). */
    @Column(name = "liveness_score", nullable = false)
    private double livenessScore;

    /** Risque de fraude documentaire, 0..1 (plus bas = meilleur). */
    @Column(name = "fraud_score", nullable = false)
    private double fraudScore;

    /** Cohérence OCR / données déclarées, 0..1 (plus haut = meilleur). */
    @Column(name = "consistency_score", nullable = false)
    private double consistencyScore;

    /** Statut proposé par le pipeline (APPROVED, REJECTED ou MANUAL_REVIEW). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus outcome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public KycCase getKycCase() { return kycCase; }
    public void setKycCase(KycCase kycCase) { this.kycCase = kycCase; }
    public double getFaceMatchScore() { return faceMatchScore; }
    public void setFaceMatchScore(double faceMatchScore) { this.faceMatchScore = faceMatchScore; }
    public double getLivenessScore() { return livenessScore; }
    public void setLivenessScore(double livenessScore) { this.livenessScore = livenessScore; }
    public double getFraudScore() { return fraudScore; }
    public void setFraudScore(double fraudScore) { this.fraudScore = fraudScore; }
    public double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(double consistencyScore) { this.consistencyScore = consistencyScore; }
    public CaseStatus getOutcome() { return outcome; }
    public void setOutcome(CaseStatus outcome) { this.outcome = outcome; }
    public Instant getCreatedAt() { return createdAt; }
}
