package com.pfe.ekyc.storage;

import com.pfe.ekyc.config.KycProperties;
import com.pfe.ekyc.exception.BadRequestException;
import com.pfe.ekyc.exception.NotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov");

    private final Path root;

    public LocalFileStorageService(KycProperties props) {
        this.root = Path.of(props.storage().root()).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long caseId, String category, MultipartFile file) {
        // Le nom d'origine n'est jamais utilisé : nom généré + extension déduite du type MIME (anti path traversal).
        String extension = EXTENSIONS.getOrDefault(file.getContentType(), ".bin");
        String relative = "case-" + caseId + "/" + category + "-" + UUID.randomUUID() + extension;
        Path target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Échec d'enregistrement du fichier", e);
        }
        return relative;
    }

    @Override
    public Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new BadRequestException("Chemin de fichier invalide");
        }
        return path;
    }

    @Override
    public Resource load(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.exists(path)) {
            throw new NotFoundException("Fichier introuvable");
        }
        return new FileSystemResource(path);
    }
}
