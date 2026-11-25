package com.pfe.ekyc.dto;

import com.pfe.ekyc.domain.CaseStatus;
import com.pfe.ekyc.domain.DocumentType;
import com.pfe.ekyc.domain.MediaKind;
import com.pfe.ekyc.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Objets d'entrée/sortie de l'API REST. */
public final class Dtos {

    private Dtos() {}

    // ---- Auth ----
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String fullName) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String tokenType, long expiresInSeconds,
                               String email, String fullName, Role role) {}

    // ---- Dossier KYC ----
    public record CreateCaseRequest(
            @NotBlank String declaredFullName,
            @NotBlank String declaredIdNumber,
            @NotNull @Past LocalDate declaredBirthDate) {}

    public record DocumentInfo(Long id, DocumentType type,
                               String ocrFullName, String ocrIdNumber, LocalDate ocrBirthDate) {}

    public record MediaInfo(Long id, MediaKind kind) {}

    public record ResultInfo(double faceMatchScore, double livenessScore, double fraudScore,
                             double consistencyScore, CaseStatus outcome) {}

    public record CaseResponse(Long id, CaseStatus status, String clientEmail,
                               String declaredFullName, String declaredIdNumber, LocalDate declaredBirthDate,
                               Instant createdAt, Instant updatedAt,
                               String decisionComment, String decidedBy,
                               List<DocumentInfo> documents, List<MediaInfo> media, ResultInfo result) {}

    // ---- Back-office ----
    public enum Decision { APPROVE, REJECT }

    public record DecisionRequest(@NotNull Decision decision, @Size(max = 500) String comment) {}

    public record AuditResponse(Long id, String actor, String action, Long caseId,
                                String details, Instant createdAt) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                    p.getTotalElements(), p.getTotalPages());
        }
    }
}
