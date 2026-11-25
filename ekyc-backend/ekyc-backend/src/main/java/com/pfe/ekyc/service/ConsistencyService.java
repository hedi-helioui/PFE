package com.pfe.ekyc.service;

import com.pfe.ekyc.ai.DeclaredIdentity;
import com.pfe.ekyc.ai.OcrResult;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

/** Vérification de cohérence entre les données lues par l'OCR et celles déclarées dans le formulaire. */
@Service
public class ConsistencyService {

    /** Moyenne de trois contrôles (nom, numéro de document, date de naissance), entre 0 et 1. */
    public double score(OcrResult ocr, DeclaredIdentity declared) {
        double name = nameSimilarity(ocr.fullName(), declared.fullName());
        double id = sameText(ocr.idNumber(), declared.idNumber()) ? 1.0 : 0.0;
        double birth = ocr.birthDate() != null && ocr.birthDate().equals(declared.birthDate()) ? 1.0 : 0.0;
        return (name + id + birth) / 3.0;
    }

    /** Similarité de Jaccard sur les mots du nom (insensible à l'ordre, aux accents et à la casse). */
    private double nameSimilarity(String a, String b) {
        Set<String> ta = tokens(a);
        Set<String> tb = tokens(b);
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(ta);
        inter.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) inter.size() / union.size();
    }

    private Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", " ")
                .trim();
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
