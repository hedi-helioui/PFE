package com.pfe.ekyc.controller;

import com.pfe.ekyc.domain.DocumentType;
import com.pfe.ekyc.dto.Dtos.CaseResponse;
import com.pfe.ekyc.dto.Dtos.CreateCaseRequest;
import com.pfe.ekyc.security.AuthUser;
import com.pfe.ekyc.service.KycCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc/cases")
@Tag(name = "Dossier KYC (client)")
public class KycController {

    private final KycCaseService service;

    public KycController(KycCaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ouvrir un dossier KYC avec l'identité déclarée")
    public CaseResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateCaseRequest request) {
        return service.create(user, request);
    }

    @GetMapping
    @Operation(summary = "Lister mes dossiers")
    public List<CaseResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return service.listMine(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un de mes dossiers (statut, scores)")
    public CaseResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @PostMapping(value = "/{id}/selfie", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envoyer le selfie (JPEG/PNG)")
    public CaseResponse selfie(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @RequestPart("file") MultipartFile file) {
        return service.addSelfie(user, id, file);
    }

    @PostMapping(value = "/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envoyer la vidéo de vivacité (MP4/WebM/MOV)")
    public CaseResponse video(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                              @RequestPart("file") MultipartFile file) {
        return service.addVideo(user, id, file);
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envoyer un document d'identité (CIN ou PASSPORT, JPEG/PNG)")
    public CaseResponse document(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                 @RequestParam(defaultValue = "CIN") DocumentType type,
                                 @RequestPart("file") MultipartFile file) {
        return service.addDocument(user, id, type, file);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre le dossier : lance la vérification automatique")
    public CaseResponse submit(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.submit(user, id);
    }
}
