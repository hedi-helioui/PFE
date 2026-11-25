package com.pfe.ekyc.service;

import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.domain.DocumentType;
import com.pfe.ekyc.domain.KycCase;
import com.pfe.ekyc.domain.KycDocument;
import com.pfe.ekyc.domain.KycMedia;
import com.pfe.ekyc.domain.MediaKind;
import com.pfe.ekyc.domain.User;
import com.pfe.ekyc.domain.VerificationResult;
import com.pfe.ekyc.dto.Dtos.CaseResponse;
import com.pfe.ekyc.dto.Dtos.CreateCaseRequest;
import com.pfe.ekyc.exception.BadRequestException;
import com.pfe.ekyc.exception.ConflictException;
import com.pfe.ekyc.exception.NotFoundException;
import com.pfe.ekyc.repository.KycCaseRepository;
import com.pfe.ekyc.repository.UserRepository;
import com.pfe.ekyc.security.AuthUser;
import com.pfe.ekyc.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/** Parcours client : création du dossier, envoi des pièces, soumission. */
@Service
public class KycCaseService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    private final KycCaseRepository cases;
    private final UserRepository users;
    private final FileStorageService storage;
    private final VerificationPipeline pipeline;
    private final AuditService audit;

    public KycCaseService(KycCaseRepository cases, UserRepository users, FileStorageService storage,
                          VerificationPipeline pipeline, AuditService audit) {
        this.cases = cases;
        this.users = users;
        this.storage = storage;
        this.pipeline = pipeline;
        this.audit = audit;
    }

    @Transactional
    public CaseResponse create(AuthUser auth, CreateCaseRequest request) {
        User user = users.findById(auth.id()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        KycCase kycCase = new KycCase();
        kycCase.setUser(user);
        kycCase.setDeclaredFullName(request.declaredFullName().trim());
        kycCase.setDeclaredIdNumber(request.declaredIdNumber().trim());
        kycCase.setDeclaredBirthDate(request.declaredBirthDate());
        kycCase = cases.saveAndFlush(kycCase);
        audit.log(auth.email(), "CASE_CREATED", kycCase.getId(), null);
        return CaseMapper.toResponse(kycCase);
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listMine(AuthUser auth) {
        return cases.findByUserIdOrderByCreatedAtDesc(auth.id()).stream().map(CaseMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse get(AuthUser auth, Long caseId) {
        return CaseMapper.toResponse(ownedCase(auth, caseId));
    }

    @Transactional
    public CaseResponse addSelfie(AuthUser auth, Long caseId, MultipartFile file) {
        KycCase kycCase = editableCase(auth, caseId);
        requireContentType(file, IMAGE_TYPES, "Le selfie doit être une image JPEG ou PNG");
        replaceMedia(kycCase, MediaKind.SELFIE, file);
        audit.log(auth.email(), "SELFIE_UPLOADED", caseId, null);
        return CaseMapper.toResponse(cases.saveAndFlush(kycCase));
    }

    @Transactional
    public CaseResponse addVideo(AuthUser auth, Long caseId, MultipartFile file) {
        KycCase kycCase = editableCase(auth, caseId);
        requireContentType(file, VIDEO_TYPES, "La vidéo doit être au format MP4, WebM ou MOV");
        replaceMedia(kycCase, MediaKind.VIDEO, file);
        audit.log(auth.email(), "VIDEO_UPLOADED", caseId, null);
        return CaseMapper.toResponse(cases.saveAndFlush(kycCase));
    }

    @Transactional
    public CaseResponse addDocument(AuthUser auth, Long caseId, DocumentType type, MultipartFile file) {
        KycCase kycCase = editableCase(auth, caseId);
        requireContentType(file, IMAGE_TYPES, "Le document doit être une image JPEG ou PNG");
        KycDocument document = new KycDocument();
        document.setKycCase(kycCase);
        document.setType(type);
        document.setContentType(file.getContentType());
        document.setFilePath(storage.store(caseId, "document-" + type.name().toLowerCase(), file));
        // Un seul document par type : le nouvel envoi remplace le précédent
        kycCase.getDocuments().removeIf(d -> d.getType() == type);
        kycCase.getDocuments().add(document);
        audit.log(auth.email(), "DOCUMENT_UPLOADED", caseId, "type=" + type);
        return CaseMapper.toResponse(cases.saveAndFlush(kycCase));
    }

    /** Soumet le dossier : lance le pipeline de vérification et fixe le statut (synchrone pour le MVP). */
    @Transactional
    public CaseResponse submit(AuthUser auth, Long caseId) {
        KycCase kycCase = editableCase(auth, caseId);
        if (kycCase.getDocuments().isEmpty()) {
            throw new BadRequestException("Un document d'identité est requis (CIN ou passeport)");
        }
        if (kycCase.getMedia().stream().noneMatch(m -> m.getKind() == MediaKind.SELFIE)) {
            throw new BadRequestException("Le selfie est requis");
        }
        if (kycCase.getMedia().stream().noneMatch(m -> m.getKind() == MediaKind.VIDEO)) {
            throw new BadRequestException("La vidéo est requise");
        }
        kycCase.setStatus(CaseStatus.PROCESSING);
        audit.log(auth.email(), "CASE_SUBMITTED", caseId, null);

        VerificationResult result = pipeline.run(kycCase);
        kycCase.setResult(result);
        kycCase.setStatus(result.getOutcome());
        audit.log("system", "VERIFICATION_COMPLETED", caseId, String.format(
                "face=%.2f liveness=%.2f fraud=%.2f consistency=%.2f outcome=%s",
                result.getFaceMatchScore(), result.getLivenessScore(), result.getFraudScore(),
                result.getConsistencyScore(), result.getOutcome()));
        return CaseMapper.toResponse(cases.saveAndFlush(kycCase));
    }

    // ---- utilitaires ----

    private void replaceMedia(KycCase kycCase, MediaKind kind, MultipartFile file) {
        KycMedia media = new KycMedia();
        media.setKycCase(kycCase);
        media.setKind(kind);
        media.setContentType(file.getContentType());
        media.setFilePath(storage.store(kycCase.getId(), kind.name().toLowerCase(), file));
        kycCase.getMedia().removeIf(m -> m.getKind() == kind);
        kycCase.getMedia().add(media);
    }

    /** Dossier appartenant à l'utilisateur ; un dossier d'autrui est signalé comme introuvable. */
    private KycCase ownedCase(AuthUser auth, Long caseId) {
        KycCase kycCase = cases.findById(caseId).orElseThrow(() -> new NotFoundException("Dossier introuvable"));
        if (!kycCase.getUser().getId().equals(auth.id())) {
            throw new NotFoundException("Dossier introuvable");
        }
        return kycCase;
    }

    /** Dossier modifiable : uniquement tant qu'il est PENDING. */
    private KycCase editableCase(AuthUser auth, Long caseId) {
        KycCase kycCase = ownedCase(auth, caseId);
        if (kycCase.getStatus() != CaseStatus.PENDING) {
            throw new ConflictException("Le dossier n'est plus modifiable (statut " + kycCase.getStatus() + ")");
        }
        return kycCase;
    }

    private void requireContentType(MultipartFile file, Set<String> allowed, String message) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier manquant ou vide");
        }
        if (file.getContentType() == null || !allowed.contains(file.getContentType())) {
            throw new BadRequestException(message);
        }
    }
}
