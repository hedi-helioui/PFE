package com.pfe.ekyc.service;

import com.pfe.ekyc.ai.DeclaredIdentity;
import com.pfe.ekyc.ai.FaceMatchService;
import com.pfe.ekyc.ai.FraudDetectionService;
import com.pfe.ekyc.ai.LivenessService;
import com.pfe.ekyc.ai.OcrResult;
import com.pfe.ekyc.ai.OcrService;
import com.pfe.ekyc.config.KycProperties;
import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.domain.KycCase;
import com.pfe.ekyc.domain.KycDocument;
import com.pfe.ekyc.domain.KycMedia;
import com.pfe.ekyc.domain.MediaKind;
import com.pfe.ekyc.domain.VerificationResult;
import com.pfe.ekyc.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Pipeline de vérification : OCR, cohérence, fraude documentaire, face match, liveness,
 * puis décision (APPROVED / REJECTED / MANUAL_REVIEW) selon les seuils de configuration.
 */
@Service
public class VerificationPipeline {

    private final OcrService ocrService;
    private final FraudDetectionService fraudService;
    private final FaceMatchService faceMatchService;
    private final LivenessService livenessService;
    private final ConsistencyService consistencyService;
    private final FileStorageService storage;
    private final KycProperties props;

    public VerificationPipeline(OcrService ocrService, FraudDetectionService fraudService,
                                FaceMatchService faceMatchService, LivenessService livenessService,
                                ConsistencyService consistencyService, FileStorageService storage,
                                KycProperties props) {
        this.ocrService = ocrService;
        this.fraudService = fraudService;
        this.faceMatchService = faceMatchService;
        this.livenessService = livenessService;
        this.consistencyService = consistencyService;
        this.storage = storage;
        this.props = props;
    }

    /** Exécute toutes les vérifications ; met à jour les données OCR du document et renvoie le résultat. */
    public VerificationResult run(KycCase kycCase) {
        KycDocument document = kycCase.getDocuments().get(0);
        Path documentPath = storage.resolve(document.getFilePath());
        Path selfiePath = storage.resolve(media(kycCase, MediaKind.SELFIE).getFilePath());
        Path videoPath = storage.resolve(media(kycCase, MediaKind.VIDEO).getFilePath());

        DeclaredIdentity declared = new DeclaredIdentity(
                kycCase.getDeclaredFullName(), kycCase.getDeclaredIdNumber(), kycCase.getDeclaredBirthDate());

        // 1. OCR puis cohérence avec le formulaire
        OcrResult ocr = ocrService.extract(documentPath, document.getType(), declared);
        document.setOcrFullName(ocr.fullName());
        document.setOcrIdNumber(ocr.idNumber());
        document.setOcrBirthDate(ocr.birthDate());
        double consistency = consistencyService.score(ocr, declared);

        // 2. Fraude documentaire, face match, liveness
        double fraud = fraudService.riskScore(documentPath, document.getType());
        double face = faceMatchService.match(selfiePath, documentPath);
        double liveness = livenessService.check(videoPath);

        VerificationResult result = new VerificationResult();
        result.setKycCase(kycCase);
        result.setConsistencyScore(consistency);
        result.setFraudScore(fraud);
        result.setFaceMatchScore(face);
        result.setLivenessScore(liveness);
        result.setOutcome(decide(face, liveness, fraud, consistency));
        return result;
    }

    /** Règle de décision : un score sous le seuil bas rejette, tout au-dessus du seuil haut approuve, sinon revue manuelle. */
    CaseStatus decide(double face, double liveness, double fraud, double consistency) {
        KycProperties.Thresholds t = props.thresholds();
        boolean reject = face < t.reject() || liveness < t.reject() || consistency < t.reject()
                || fraud >= t.fraudRejectMin();
        if (reject) {
            return CaseStatus.REJECTED;
        }
        boolean approve = face >= t.approve() && liveness >= t.approve() && consistency >= t.approve()
                && fraud <= t.fraudApproveMax();
        return approve ? CaseStatus.APPROVED : CaseStatus.MANUAL_REVIEW;
    }

    private KycMedia media(KycCase kycCase, MediaKind kind) {
        return kycCase.getMedia().stream()
                .filter(m -> m.getKind() == kind)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Média manquant : " + kind));
    }
}
