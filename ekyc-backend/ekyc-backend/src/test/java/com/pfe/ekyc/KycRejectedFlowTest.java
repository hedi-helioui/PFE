package com.pfe.ekyc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Score de fraude élevé (0.9) : rejet automatique. */
@SpringBootTest(properties = "kyc.mock.fraud-score=0.9")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KycRejectedFlowTest extends AbstractApiTest {

    @Test
    void highFraudScoreIsRejectedAutomatically() throws Exception {
        String token = registerClient();
        long caseId = createCase(token);
        uploadAll(token, caseId);
        assertThat(submit(token, caseId)).contains("\"status\":\"REJECTED\"");
    }
}
