package com.pfe.ekyc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Outils communs aux tests de bout en bout de l'API. */
abstract class AbstractApiTest {

    protected static final String PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String registerClient() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.com";
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "password", PASSWORD, "fullName", "Mohamed Ben Ali"));
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    protected String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    protected long createCase(String token) throws Exception {
        String body = """
                {"declaredFullName":"Mohamed Ben Ali","declaredIdNumber":"12345678","declaredBirthDate":"1998-05-14"}
                """;
        String json = mvc.perform(post("/api/v1/kyc/cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }

    protected void uploadAll(String token, long caseId) throws Exception {
        upload(token, caseId, "selfie", "image/jpeg");
        upload(token, caseId, "video", "video/mp4");
        upload(token, caseId, "document", "image/png");
    }

    protected void upload(String token, long caseId, String part, String contentType) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x", contentType, new byte[]{1, 2, 3, 4});
        mvc.perform(multipart("/api/v1/kyc/cases/{id}/{part}", caseId, part).file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    protected String submit(String token, long caseId) throws Exception {
        return mvc.perform(post("/api/v1/kyc/cases/{id}/submit", caseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
