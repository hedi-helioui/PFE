package com.pfe.ekyc.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Stockage des fichiers (selfies, vidéos, documents).
 * Implémentation actuelle : disque local. Remplaçable par MinIO / S3 sans toucher au reste.
 */
public interface FileStorageService {

    /** Enregistre le fichier et renvoie son chemin relatif (à stocker en base). */
    String store(Long caseId, String category, MultipartFile file);

    /** Chemin absolu sur disque, utilisé par les services IA. */
    Path resolve(String relativePath);

    /** Contenu du fichier, pour le téléchargement par le back-office. */
    Resource load(String relativePath);
}
