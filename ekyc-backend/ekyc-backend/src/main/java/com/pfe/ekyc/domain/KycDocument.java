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

import java.time.LocalDate;

@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private KycCase kycCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocumentType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // Données extraites par l'OCR
    @Column(name = "ocr_full_name")
    private String ocrFullName;

    @Column(name = "ocr_id_number")
    private String ocrIdNumber;

    @Column(name = "ocr_birth_date")
    private LocalDate ocrBirthDate;

    public Long getId() { return id; }
    public KycCase getKycCase() { return kycCase; }
    public void setKycCase(KycCase kycCase) { this.kycCase = kycCase; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getOcrFullName() { return ocrFullName; }
    public void setOcrFullName(String ocrFullName) { this.ocrFullName = ocrFullName; }
    public String getOcrIdNumber() { return ocrIdNumber; }
    public void setOcrIdNumber(String ocrIdNumber) { this.ocrIdNumber = ocrIdNumber; }
    public LocalDate getOcrBirthDate() { return ocrBirthDate; }
    public void setOcrBirthDate(LocalDate ocrBirthDate) { this.ocrBirthDate = ocrBirthDate; }
}
