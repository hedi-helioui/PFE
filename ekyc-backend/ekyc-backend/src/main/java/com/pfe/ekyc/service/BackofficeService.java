package com.pfe.ekyc.service;

import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.domain.KycCase;
import com.pfe.ekyc.dto.Dtos.AuditResponse;
import com.pfe.ekyc.dto.Dtos.CaseResponse;
import com.pfe.ekyc.dto.Dtos.Decision;
import com.pfe.ekyc.dto.Dtos.DecisionRequest;
import com.pfe.ekyc.dto.Dtos.PageResponse;
import com.pfe.ekyc.exception.BadRequestException;
import com.pfe.ekyc.exception.ConflictException;
import com.pfe.ekyc.exception.NotFoundException;
import com.pfe.ekyc.repository.AuditLogRepository;
import com.pfe.ekyc.repository.KycCaseRepository;
import com.pfe.ekyc.security.AuthUser;
import com.pfe.ekyc.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Parcours agent : file de validation manuelle, consultation, décision, audit. */
@Service
public class BackofficeService {

    public record FileContent(Resource resource, String contentType) {}

    private final KycCaseRepository cases;
    private final AuditLogRepository auditLogs;
    private final FileStorageService storage;
    private final AuditService audit;

    public BackofficeService(KycCaseRepository cases, AuditLogRepository auditLogs,
                             FileStorageService storage, AuditService audit) {
        this.cases = cases;
        this.auditLogs = auditLogs;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseResponse> list(CaseStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KycCase> result = status == null ? cases.findAll(pageable) : cases.findByStatus(status, pageable);
        return PageResponse.from(result.map(CaseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CaseResponse get(Long caseId) {
        return CaseMapper.toResponse(find(caseId));
    }

    /** Décision humaine : possible uniquement sur un dossier en MANUAL_REVIEW. */
    @Transactional
    public CaseResponse decide(AuthUser agent, Long caseId, DecisionRequest request) {
        KycCase kycCase = find(caseId);
        if (kycCase.getStatus() != CaseStatus.MANUAL_REVIEW) {
            throw new ConflictException("Seuls les dossiers en MANUAL_REVIEW peuvent être décidés (statut "
                    + kycCase.getStatus() + ")");
        }
        boolean approve = request.decision() == Decision.APPROVE;
        if (!approve && (request.comment() == null || request.comment().isBlank())) {
            throw new BadRequestException("Un commentaire est obligatoire pour rejeter un dossier");
        }
        kycCase.setStatus(approve ? CaseStatus.APPROVED : CaseStatus.REJECTED);
        kycCase.setDecisionComment(request.comment());
        kycCase.setDecidedBy(agent.email());
        audit.log(agent.email(), "MANUAL_DECISION", caseId,
                request.decision() + (request.comment() == null ? "" : " : " + request.comment()));
        return CaseMapper.toResponse(cases.saveAndFlush(kycCase));
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> auditTrail(Long caseId) {
        find(caseId);
        return auditLogs.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(a -> new AuditResponse(a.getId(), a.getActor(), a.getAction(), a.getCaseId(),
                        a.getDetails(), a.getCreatedAt()))
                .toList();
    }

    // Pas en lecture seule : la consultation d'une pièce est tracée dans le journal d'audit (écriture)
    @Transactional
    public FileContent documentFile(AuthUser agent, Long caseId, Long documentId) {
        KycCase kycCase = find(caseId);
        var document = kycCase.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Document introuvable"));
        audit.log(agent.email(), "DOCUMENT_VIEWED", caseId, "documentId=" + documentId);
        return new FileContent(storage.load(document.getFilePath()), document.getContentType());
    }

    @Transactional
    public FileContent mediaFile(AuthUser agent, Long caseId, Long mediaId) {
        KycCase kycCase = find(caseId);
        var media = kycCase.getMedia().stream()
                .filter(m -> m.getId().equals(mediaId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Média introuvable"));
        audit.log(agent.email(), "MEDIA_VIEWED", caseId, "mediaId=" + mediaId);
        return new FileContent(storage.load(media.getFilePath()), media.getContentType());
    }

    private KycCase find(Long caseId) {
        return cases.findById(caseId).orElseThrow(() -> new NotFoundException("Dossier introuvable"));
    }
}
