package com.pfe.ekyc.ai;

import com.pfe.ekyc.domain.DocumentType;

import java.nio.file.Path;

public interface FraudDetectionService {

    /** Risque de falsification du document, entre 0 et 1 (0 = authentique, 1 = fraude quasi certaine). */
    double riskScore(Path documentImage, DocumentType type);
}
