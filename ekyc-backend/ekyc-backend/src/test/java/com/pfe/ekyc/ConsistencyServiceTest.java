package com.pfe.ekyc;

import com.pfe.ekyc.ai.DeclaredIdentity;
import com.pfe.ekyc.ai.OcrResult;
import com.pfe.ekyc.service.ConsistencyService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistencyServiceTest {

    private final ConsistencyService service = new ConsistencyService();
    private final DeclaredIdentity declared =
            new DeclaredIdentity("Mohamed Ben Ali", "12345678", LocalDate.of(1998, 5, 14));

    @Test
    void identicalDataGivesOne() {
        OcrResult ocr = new OcrResult("MOHAMED BEN ALI", "12345678", LocalDate.of(1998, 5, 14));
        assertThat(service.score(ocr, declared)).isEqualTo(1.0);
    }

    @Test
    void accentsOrderAndCaseAreIgnoredInNames() {
        DeclaredIdentity accented = new DeclaredIdentity("Hédi Hélioui", "1", LocalDate.of(2000, 1, 1));
        OcrResult ocr = new OcrResult("HELIOUI hedi", "1", LocalDate.of(2000, 1, 1));
        assertThat(service.score(ocr, accented)).isEqualTo(1.0);
    }

    @Test
    void differentIdAndBirthDateLowerTheScore() {
        OcrResult ocr = new OcrResult("Mohamed Ben Ali", "99999999", LocalDate.of(1990, 1, 1));
        assertThat(service.score(ocr, declared)).isCloseTo(1.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void missingOcrDataGivesZero() {
        assertThat(service.score(new OcrResult(null, null, null), declared)).isZero();
    }
}
