package com.pfe.ekyc.controller;

import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.dto.Dtos.AuditResponse;
import com.pfe.ekyc.dto.Dtos.CaseResponse;
import com.pfe.ekyc.dto.Dtos.DecisionRequest;
import com.pfe.ekyc.dto.Dtos.PageResponse;
import com.pfe.ekyc.security.AuthUser;
import com.pfe.ekyc.service.BackofficeService;
import com.pfe.ekyc.service.BackofficeService.FileContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/backoffice/cases")
@Tag(name = "Back-office (agent)")
public class BackofficeController {

    private final BackofficeService service;

    public BackofficeController(BackofficeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les dossiers, filtrables par statut (ex. MANUAL_REVIEW)")
    public PageResponse<CaseResponse> list(@RequestParam(required = false) CaseStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un dossier avec scores et données OCR")
    public CaseResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Approuver ou rejeter un dossier en revue manuelle")
    public CaseResponse decide(@AuthenticationPrincipal AuthUser agent, @PathVariable Long id,
                               @Valid @RequestBody DecisionRequest request) {
        return service.decide(agent, id, request);
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Journal d'audit du dossier")
    public List<AuditResponse> audit(@PathVariable Long id) {
        return service.auditTrail(id);
    }

    @GetMapping("/{id}/documents/{documentId}/file")
    @Operation(summary = "Télécharger l'image d'un document")
    public ResponseEntity<Resource> documentFile(@AuthenticationPrincipal AuthUser agent,
                                                 @PathVariable Long id, @PathVariable Long documentId) {
        return toResponse(service.documentFile(agent, id, documentId));
    }

    @GetMapping("/{id}/media/{mediaId}/file")
    @Operation(summary = "Télécharger le selfie ou la vidéo")
    public ResponseEntity<Resource> mediaFile(@AuthenticationPrincipal AuthUser agent,
                                              @PathVariable Long id, @PathVariable Long mediaId) {
        return toResponse(service.mediaFile(agent, id, mediaId));
    }

    private ResponseEntity<Resource> toResponse(FileContent content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .body(content.resource());
    }
}
