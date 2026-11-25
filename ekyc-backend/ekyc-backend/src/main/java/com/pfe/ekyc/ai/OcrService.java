package com.pfe.ekyc.ai;

import com.pfe.ekyc.domain.DocumentType;

import java.nio.file.Path;

public interface OcrService {

    /**
     * Extrait nom, numéro et date de naissance du document (CIN ou passeport).
     *
     * @param hint identité déclarée : utilisée UNIQUEMENT par l'implémentation mock pour permettre une démo ;
     *             une vraie implémentation doit l'ignorer.
     */
    OcrResult extract(Path documentImage, DocumentType type, DeclaredIdentity hint);
}
