package com.pfe.ekyc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Face match intermédiaire (0.65) : le dossier part en revue manuelle et l'agent tranche. */
@SpringBootTest(properties = "kyc.mock.face-match-score=0.65")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KycManualReviewFlowTest extends AbstractApiTest {

    @Test
    void agentApprovesManualReviewCase() throws Exception {
        String clientToken = registerClient();
        long caseId = createCase(clientToken);
        uploadAll(clientToken, caseId);
        org.assertj.core.api.Assertions.assertThat(submit(clientToken, caseId)).contains("\"status\":\"MANUAL_REVIEW\"");

        String agentToken = login("agent@kyc.local", "Agent123!");

        mvc.perform(get("/api/v1/backoffice/cases").param("status", "MANUAL_REVIEW")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) caseId)));

        mvc.perform(post("/api/v1/backoffice/cases/{id}/decision", caseId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"comment\":\"Visage confirmé manuellement\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("agent@kyc.local"));

        // Une seconde décision est refusée
        mvc.perform(post("/api/v1/backoffice/cases/{id}/decision", caseId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"comment\":\"trop tard\"}"))
                .andExpect(status().isConflict());

        // L'audit contient la soumission, la vérification et la décision
        mvc.perform(get("/api/v1/backoffice/cases/{id}/audit", caseId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("CASE_SUBMITTED")))
                .andExpect(jsonPath("$[*].action", hasItem("VERIFICATION_COMPLETED")))
                .andExpect(jsonPath("$[*].action", hasItem("MANUAL_DECISION")));
    }

    @Test
    void rejectionRequiresAComment() throws Exception {
        String clientToken = registerClient();
        long caseId = createCase(clientToken);
        uploadAll(clientToken, caseId);
        submit(clientToken, caseId);

        String agentToken = login("agent@kyc.local", "Agent123!");
        mvc.perform(post("/api/v1/backoffice/cases/{id}/decision", caseId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isBadRequest());
    }
}
