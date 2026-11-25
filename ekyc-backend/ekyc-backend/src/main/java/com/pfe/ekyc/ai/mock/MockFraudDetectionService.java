package com.pfe.ekyc.ai.mock;

import com.pfe.ekyc.ai.FraudDetectionService;
import com.pfe.ekyc.config.KycProperties;
import com.pfe.ekyc.domain.DocumentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "kyc.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockFraudDetectionService implements FraudDetectionService {

    private final KycProperties props;

    public MockFraudDetectionService(KycProperties props) {
        this.props = props;
    }

    @Override
    public double riskScore(Path documentImage, DocumentType type) {
        return props.mock().fraudScore();
    }
}
