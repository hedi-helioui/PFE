package com.pfe.ekyc.ai;

import java.nio.file.Path;

public interface LivenessService {

    /** Score de vivacité de la vidéo, entre 0 et 1 (1 = personne bien réelle et présente). */
    double check(Path video);
}
