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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "kyc_media")
public class KycMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private KycCase kycCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false)
    private MediaKind kind;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    public Long getId() { return id; }
    public KycCase getKycCase() { return kycCase; }
    public void setKycCase(KycCase kycCase) { this.kycCase = kycCase; }
    public MediaKind getKind() { return kind; }
    public void setKind(MediaKind kind) { this.kind = kind; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
