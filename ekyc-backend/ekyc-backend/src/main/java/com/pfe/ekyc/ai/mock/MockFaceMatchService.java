package com.pfe.ekyc.ai.mock;

import com.pfe.ekyc.ai.FaceMatchService;
import com.pfe.ekyc.config.KycProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "kyc.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockFaceMatchService implements FaceMatchService {

    private final KycProperties props;

    public MockFaceMatchService(KycProperties props) {
        this.props = props;
    }

    @Override
    public double match(Path selfie, Path documentImage) {
        return props.mock().faceMatchScore();
    }
}
