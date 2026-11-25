package com.pfe.ekyc.service;

import com.pfe.ekyc.domain.KycCase;
import com.pfe.ekyc.domain.VerificationResult;
import com.pfe.ekyc.dto.Dtos.CaseResponse;
import com.pfe.ekyc.dto.Dtos.DocumentInfo;
import com.pfe.ekyc.dto.Dtos.MediaInfo;
import com.pfe.ekyc.dto.Dtos.ResultInfo;

/** À appeler dans une transaction ouverte (les collections du dossier sont chargées paresseusement). */
public final class CaseMapper {

    private CaseMapper() {}

    public static CaseResponse toResponse(KycCase c) {
        VerificationResult r = c.getResult();
        ResultInfo result = r == null ? null : new ResultInfo(
                r.getFaceMatchScore(), r.getLivenessScore(), r.getFraudScore(),
                r.getConsistencyScore(), r.getOutcome());
        return new CaseResponse(
                c.getId(), c.getStatus(), c.getUser().getEmail(),
                c.getDeclaredFullName(), c.getDeclaredIdNumber(), c.getDeclaredBirthDate(),
                c.getCreatedAt(), c.getUpdatedAt(),
                c.getDecisionComment(), c.getDecidedBy(),
                c.getDocuments().stream()
                        .map(d -> new DocumentInfo(d.getId(), d.getType(),
                                d.getOcrFullName(), d.getOcrIdNumber(), d.getOcrBirthDate()))
                        .toList(),
                c.getMedia().stream().map(m -> new MediaInfo(m.getId(), m.getKind())).toList(),
                result);
    }
}
