package com.pfe.ekyc.ai.mock;

import com.pfe.ekyc.ai.DeclaredIdentity;
import com.pfe.ekyc.ai.OcrResult;
import com.pfe.ekyc.ai.OcrService;
import com.pfe.ekyc.domain.DocumentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Mock : "lit" sur le document exactement ce que le client a déclaré. */
@Component
@ConditionalOnProperty(name = "kyc.ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockOcrService implements OcrService {

    @Override
    public OcrResult extract(Path documentImage, DocumentType type, DeclaredIdentity hint) {
        return new OcrResult(hint.fullName(), hint.idNumber(), hint.birthDate());
    }
}
