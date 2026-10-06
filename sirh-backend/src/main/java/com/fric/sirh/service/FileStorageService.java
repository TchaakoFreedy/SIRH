package com.fric.sirh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:C:/Users/Stagiaire_APS/Desktop/sirh-backend/uploads}")
    private String uploadDir;

    private static final String PAYSLIP_DIR = "payslip";

    public String getUploadDir() {
        return uploadDir;
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Fichier vide ou null");
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Répertoire de base créé: {}", uploadPath);
            }

            Path subDirPath = uploadPath.resolve(subDirectory);
            if (!Files.exists(subDirPath)) {
                Files.createDirectories(subDirPath);
                log.info("Sous-répertoire créé: {}", subDirPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + extension;

            Path filePath = subDirPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier stocké: {}", filePath.toAbsolutePath());

            // 🔥 CORRECTION : Retourner l'URL sans "/uploads/" au début
            // car le chemin de base est déjà "uploads"
            return "/" + subDirectory + "/" + uniqueFilename;

        } catch (IOException e) {
            log.error("Erreur lors du stockage du fichier: {}", e.getMessage());
            throw new RuntimeException("Erreur lors du stockage du fichier: " + e.getMessage());
        }
    }

    public String storeImage(BufferedImage image, String subDirectory, String fileName) throws IOException {
        log.info("Stockage de l'image: {}", fileName);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path subDirPath = uploadPath.resolve(subDirectory);
        if (!Files.exists(subDirPath)) {
            Files.createDirectories(subDirPath);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = UUID.randomUUID().toString() + "_" + timestamp + ".png";
        Path filePath = subDirPath.resolve(uniqueFilename);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            Files.copy(bais, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 🔥 CORRECTION : Retourner l'URL sans "/uploads/" au début
        String url = "/" + subDirectory + "/" + uniqueFilename;
        log.info("Image stockée avec URL: {}", url);
        return url;
    }

    public Path getPathFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        log.info("📂 getPathFromUrl - URL reçue: '{}'", fileUrl);

        // 🔥 CORRECTION : Nettoyer l'URL
        String relativePath = fileUrl;

        // Supprimer le préfixe /uploads/ s'il existe
        if (relativePath.startsWith("/uploads/")) {
            relativePath = relativePath.substring(9); // Supprime "/uploads/"
            log.info("📂 URL nettoyée (suppression de /uploads/): '{}'", relativePath);
        }

        // Supprimer le / initial s'il existe
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
            log.info("📂 URL nettoyée (suppression du / initial): '{}'", relativePath);
        }

        // Construire le chemin complet
        Path fullPath = Paths.get(uploadDir).resolve(relativePath);
        log.info("📂 Chemin complet résolu: '{}'", fullPath.toAbsolutePath());

        return fullPath;
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return;
            }
            Path path = getPathFromUrl(fileUrl);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Fichier supprimé: {}", path);
            } else {
                log.warn("Fichier non trouvé pour suppression: {}", path);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier: {}", e.getMessage());
        }
    }

    public void deleteDirectory(String subDirectory) {
        try {
            Path subDirPath = Paths.get(uploadDir).resolve(subDirectory);
            if (Files.exists(subDirPath)) {
                Files.walk(subDirPath)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.error("Erreur lors de la suppression du fichier: {}", path);
                            }
                        });
                log.info("Répertoire supprimé: {}", subDirPath);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du répertoire: {}", e.getMessage());
        }
    }
}