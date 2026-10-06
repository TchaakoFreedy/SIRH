package com.fric.sirh.controller;

import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Documents;
import com.fric.sirh.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/documents-management")
@RequiredArgsConstructor

public class DocumentController {

    private final DocumentService documentService;

    @Value("${file.upload-dir:C:/Users/Stagiaire_APS/Desktop/sirh-backend/uploads}")
    private String uploadDir;

    // ==========================================
    // 📄 CONTRAT ENDPOINTS
    // ==========================================

    @PostMapping("/contrats/employe/{employeeId}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Contrat> creerContrat(@PathVariable String employeeId, @RequestBody Contrat contrat) {
        String currentUser = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.creerContrat(contrat, employeeId, currentUser));
    }

    @PutMapping("/contrats/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Contrat> modifierContrat(@PathVariable String id, @RequestBody Contrat contrat) {
        String currentUser = getCurrentUser();
        return ResponseEntity.ok(documentService.modifierContrat(id, contrat, currentUser));
    }

    @PatchMapping("/contrats/{id}/resilier")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> resilierContrat(@PathVariable String id) {
        String currentUser = getCurrentUser();
        documentService.resilierContrat(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/contrats")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<Contrat>> getAllContrats() {
        return ResponseEntity.ok(documentService.getAllContrats());
    }

    @GetMapping("/contrats/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<Contrat> getContratById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getContratById(id));
    }

    // ==========================================
    // 📄 DOCUMENT ENDPOINTS - UPLOAD
    // ==========================================

    @PostMapping(value = "/pieces/employe/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<Documents> ajouterDocumentEmploye(
            @PathVariable String employeeId,
            @RequestParam("typeDocument") String typeDocument,
            @RequestParam("name") String name,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        String currentUser = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.ajouterDocumentEmploye(
                        employeeId,
                        name,
                        typeDocument,
                        null,
                        files,
                        currentUser));
    }

    @PostMapping(value = "/pieces/contrat/{contratId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<Documents> ajouterDocumentAContrat(
            @PathVariable String contratId,
            @RequestParam("typeDocument") String typeDocument,
            @RequestParam("name") String name,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        String currentUser = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.ajouterDocumentAContrat(
                        contratId,
                        name,
                        typeDocument,
                        null,
                        files,
                        currentUser));
    }

    @DeleteMapping("/pieces/{id}")
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<Void> supprimerDocument(@PathVariable String id) {
        documentService.supprimerDocument(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 📄 DOCUMENT ENDPOINTS - VIEW
    // ==========================================

    @GetMapping("/pieces/employe/{employeeId}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<List<Documents>> getDocsByEmployee(@PathVariable String employeeId) {
        log.info("Récupération de tous les documents pour l'employé ID: {}", employeeId);
        return ResponseEntity.ok(documentService.getDocumentsByEmployee(employeeId));
    }

    @GetMapping("/pieces/employe/{employeeId}/urls")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> getEmployeeDocumentUrls(@PathVariable String employeeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = auth.getName();
        String userRole = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("");

        if (userRole.contains("EMPLOYEE") && !employeeId.equals(currentUserId)) {
            throw new RuntimeException("Access denied: You can only view your own documents");
        }

        if (userRole.contains("MANAGER") && !employeeId.equals(currentUserId)) {
            if (!checkIfInManagerTeam(currentUserId, employeeId)) {
                throw new RuntimeException("Access denied: This employee is not in your team");
            }
        }

        return ResponseEntity.ok(documentService.getDocumentUrlsByEmployee(employeeId));
    }

    @GetMapping("/pieces/contrat/{contratId}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<List<Documents>> getDocsByContrat(@PathVariable String contratId) {
        log.info("Récupération des documents pour le contrat ID: {}", contratId);
        return ResponseEntity.ok(documentService.getDocumentsByContrat(contratId));
    }

    @GetMapping("/pieces/{id}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<Documents> getDocumentById(@PathVariable String id) {
        log.info("Récupération du document ID: {}", id);
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/pieces/all")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<Documents>> getAllDocuments() {
        log.info("Récupération de tous les documents");
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/pieces/type/{type}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<List<Documents>> getDocumentsByType(@PathVariable String type) {
        log.info("Récupération des documents de type: {}", type);
        return ResponseEntity.ok(documentService.getDocumentsByType(type));
    }

    @GetMapping("/pieces/employe/{employeeId}/type/{type}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<List<Documents>> getDocumentsByEmployeeAndType(
            @PathVariable String employeeId,
            @PathVariable String type) {
        log.info("Récupération des documents de type {} pour l'employé ID: {}", type, employeeId);
        return ResponseEntity.ok(documentService.getDocumentsByEmployeeAndType(employeeId, type));
    }

    // ==========================================
    // 📄 FILE DOWNLOAD & PREVIEW
    // ==========================================

    @GetMapping("/pieces/{id}/file")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {
        Documents doc = documentService.getDocumentById(id);

        if (doc.getImageUrls() == null || doc.getImageUrls().isEmpty()) {
            throw new RuntimeException("Aucun fichier associé");
        }

        String fileUrl = doc.getImageUrls().get(0);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        log.info("📥 Téléchargement du fichier: {}", fileName);

        Path filePath = findFileInUploadDirectories(fileName);

        if (filePath == null) {
            throw new RuntimeException("Fichier introuvable sur le serveur : " + fileName);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                throw new RuntimeException("Fichier non lisible : " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur de chemin de fichier", e);
        }
    }

    @GetMapping("/pieces/{id}/preview")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<Resource> previewFile(@PathVariable String id) {
        Documents doc = documentService.getDocumentById(id);

        if (doc.getImageUrls() == null || doc.getImageUrls().isEmpty()) {
            throw new RuntimeException("Aucun fichier associé");
        }

        String fileUrl = doc.getImageUrls().get(0);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        log.info("👁️ Prévisualisation du fichier: {}", fileName);

        Path filePath = findFileInUploadDirectories(fileName);

        if (filePath == null) {
            Path directPath = Paths.get(uploadDir).resolve(fileName).normalize();
            if (Files.exists(directPath)) {
                log.info("✅ Fichier trouvé directement dans uploads: {}", directPath);
                filePath = directPath;
            } else {
                log.error("❌ Fichier non trouvé: {}", fileName);
                throw new RuntimeException("Fichier introuvable pour la prévisualisation : " + fileName);
            }
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                log.info("✅ Prévisualisation du fichier: {}", filePath.toAbsolutePath());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                throw new RuntimeException("Fichier non lisible : " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur de chemin de fichier", e);
        }
    }

    @GetMapping("/files/download")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<Resource> downloadFileByUrl(@RequestParam String url) {
        log.info("Téléchargement du fichier depuis l'URL: {}", url);
        try {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            Path filePath = findFileInUploadDirectories(fileName);

            if (filePath == null) {
                throw new RuntimeException("Fichier introuvable: " + fileName);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                throw new RuntimeException("Fichier non lisible: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur de chemin de fichier", e);
        }
    }

    // ==========================================
    // 🔧 UTILITY ENDPOINTS
    // ==========================================

    @GetMapping("/debug/files")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Map<String, Object>> debugFiles() {
        Map<String, Object> response = new HashMap<>();
        Path uploadsPath = Paths.get(uploadDir);

        try {
            if (Files.exists(uploadsPath)) {
                Map<String, List<String>> filesByDirectory = new HashMap<>();
                List<String> rootFiles = Files.list(uploadsPath)
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                filesByDirectory.put("root", rootFiles);

                List<Path> subDirs = Files.list(uploadsPath)
                        .filter(Files::isDirectory)
                        .collect(Collectors.toList());

                for (Path subDir : subDirs) {
                    List<String> subFiles = Files.list(subDir)
                            .filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .collect(Collectors.toList());
                    filesByDirectory.put(subDir.getFileName().toString(), subFiles);
                }

                response.put("directory", uploadsPath.toAbsolutePath().toString());
                response.put("exists", true);
                response.put("files", filesByDirectory);
                response.put("totalCount", filesByDirectory.values().stream().mapToInt(List::size).sum());
            } else {
                response.put("exists", false);
                response.put("directory", uploadsPath.toAbsolutePath().toString());
                response.put("message", "Uploads directory does not exist");
            }
        } catch (IOException e) {
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fix-document/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Map<String, Object>> fixDocumentPath(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Documents doc = documentService.getDocumentById(id);
            if (doc.getImageUrls() != null && !doc.getImageUrls().isEmpty()) {
                String oldPath = doc.getImageUrls().get(0);
                String fileName = oldPath.substring(oldPath.lastIndexOf('/') + 1);
                Path foundPath = findFileInUploadDirectories(fileName);

                if (foundPath != null) {
                    String relativePath = "/uploads/" + foundPath.getParent().getFileName().toString() + "/" + fileName;
                    List<String> newUrls = Collections.singletonList(relativePath);
                    doc.setImageUrls(newUrls);
                    documentService.updateDocument(doc);

                    response.put("success", true);
                    response.put("documentId", id);
                    response.put("oldPath", oldPath);
                    response.put("newPath", relativePath);
                    response.put("absolutePath", foundPath.toAbsolutePath().toString());
                    response.put("message", "Document path fixed successfully");
                } else {
                    response.put("success", false);
                    response.put("message", "File not found on server");
                    response.put("fileName", fileName);
                    response.put("searchDirectory", uploadDir);
                }
            } else {
                response.put("success", false);
                response.put("message", "No image URLs found for this document");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/employee/{employeeId}")
    @PreAuthorize("hasAuthority('DOC_VIEW')")
    public ResponseEntity<Map<String, Object>> getDocumentStatsForEmployee(@PathVariable String employeeId) {
        List<Documents> docs = documentService.getDocumentsByEmployee(employeeId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", docs.size());

        Map<String, Long> byType = docs.stream()
                .collect(Collectors.groupingBy(Documents::getTypeDocument, Collectors.counting()));
        stats.put("byType", byType);

        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // 🔧 PRIVATE METHODS
    // ==========================================

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    private boolean checkIfInManagerTeam(String managerId, String employeeId) {
        // À implémenter selon la logique métier (ex: vérifier si l'employé est dans l'équipe du manager)
        return true;
    }

    private String determineContentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        } else if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "application/msword";
        } else if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private Path findFileInUploadDirectories(String fileName) {
        String[] directories = {"photos", "cni", "certificats", "documents", "contrats", "autres", ""};

        for (String dir : directories) {
            Path testPath;
            if (dir.isEmpty()) {
                testPath = Paths.get(uploadDir).resolve(fileName).normalize();
            } else {
                testPath = Paths.get(uploadDir).resolve(dir).resolve(fileName).normalize();
            }

            if (Files.exists(testPath)) {
                log.info("✅ Fichier trouvé dans '{}': {}", dir, testPath);
                return testPath;
            }
        }

        // Recherche récursive si non trouvé
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (Files.exists(uploadPath)) {
                List<Path> foundFiles = Files.walk(uploadPath)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals(fileName))
                        .collect(Collectors.toList());

                if (!foundFiles.isEmpty()) {
                    log.info("✅ Fichier trouvé par recherche récursive: {}", foundFiles.get(0));
                    return foundFiles.get(0);
                }
            }
        } catch (IOException e) {
            log.error("Erreur lors de la recherche récursive: {}", e.getMessage());
        }

        return null;
    }
}