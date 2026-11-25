package com.pfe.ekyc.ai;

import java.nio.file.Path;

public interface FaceMatchService {

    /** Similarité entre le visage du selfie et celui du document, entre 0 et 1 (1 = même personne). */
    double match(Path selfie, Path documentImage);
}
