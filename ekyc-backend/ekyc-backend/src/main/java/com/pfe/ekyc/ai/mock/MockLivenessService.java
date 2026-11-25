package com.pfe.ekyc.ai.mock;

import com.pfe.ekyc.ai.LivenessService;
import com.pfe.ekyc.config.KycProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "kyc.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockLivenessService implements LivenessService {

    private final KycProperties props;

    public MockLivenessService(KycProperties props) {
        this.props = props;
    }

    @Override
    public double check(Path video) {
        return props.mock().livenessScore();
    }
}
