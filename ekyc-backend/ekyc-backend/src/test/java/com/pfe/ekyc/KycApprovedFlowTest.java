package com.pfe.ekyc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KycApprovedFlowTest extends AbstractApiTest {

    @Test
    void happyPathEndsApproved() throws Exception {
        String token = registerClient();
        long caseId = createCase(token);
        uploadAll(token, caseId);

        String json = submit(token, caseId);
        org.assertj.core.api.Assertions.assertThat(json).contains("\"status\":\"APPROVED\"");

        mvc.perform(get("/api/v1/kyc/cases/{id}", caseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.result.consistencyScore").value(1.0))
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].ocrIdNumber").value("12345678"));
    }

    @Test
    void submitWithoutFilesIsRejectedWith400() throws Exception {
        String token = registerClient();
        long caseId = createCase(token);
        mvc.perform(post("/api/v1/kyc/cases/{id}/submit", caseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void caseIsNotEditableAfterSubmission() throws Exception {
        String token = registerClient();
        long caseId = createCase(token);
        uploadAll(token, caseId);
        submit(token, caseId);
        mvc.perform(post("/api/v1/kyc/cases/{id}/submit", caseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherClientCannotSeeMyCase() throws Exception {
        String owner = registerClient();
        String other = registerClient();
        long caseId = createCase(owner);
        mvc.perform(get("/api/v1/kyc/cases/{id}", caseId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFileTypeIsRefused() throws Exception {
        String token = registerClient();
        long caseId = createCase(token);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1});
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/kyc/cases/{id}/selfie", caseId).file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void securityRulesAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/kyc/cases")).andExpect(status().isUnauthorized());

        String clientToken = registerClient();
        mvc.perform(get("/api/v1/backoffice/cases").header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
