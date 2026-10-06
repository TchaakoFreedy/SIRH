package com.fric.sirh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String storeImage(MultipartFile file, String folder, String entityId) throws IOException {
        // Créer le répertoire si nécessaire
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = uploadDir + "/" + folder + "/" + entityId + "/" + dateFolder;
        Path uploadPath = Paths.get(basePath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générer un nom unique pour le fichier
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Copier le fichier
        Files.copy(file.getInputStream(), filePath);

        // Retourner le chemin relatif
        String relativePath = folder + "/" + entityId + "/" + dateFolder + "/" + filename;
        log.info("Fichier stocké: {}", relativePath);

        return relativePath;
    }

    // Nouvelle méthode : stocker un fichier sans entité (pour l'upload de PDF global)
    public String storeFile(MultipartFile file, String folder) throws IOException {
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = uploadDir + "/" + folder + "/" + dateFolder;
        Path uploadPath = Paths.get(basePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        String relativePath = folder + "/" + dateFolder + "/" + filename;
        return relativePath;
    }

    public byte[] downloadImage(String imageUrl) throws IOException {
        Path filePath = Paths.get(uploadDir, imageUrl);
        if (!Files.exists(filePath)) {
            throw new IOException("Fichier non trouvé: " + imageUrl);
        }
        return Files.readAllBytes(filePath);
    }

    // Nouvelle méthode : télécharger un fichier en tant que Resource
    public Resource downloadResource(String fileUrl) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir, fileUrl);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Fichier non trouvé: " + fileUrl);
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Impossible de lire le fichier: " + fileUrl);
        }
    }

    public void deleteImage(String imageUrl) throws IOException {
        Path filePath = Paths.get(uploadDir, imageUrl);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Fichier supprimé: {}", imageUrl);
        }
    }
}