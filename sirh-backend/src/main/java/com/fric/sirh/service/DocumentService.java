package com.fric.sirh.service;

import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Documents;
import com.fric.sirh.model.Employee;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.DocumentsRepository;
import com.fric.sirh.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ContratRepository contratRepository;
    private final DocumentsRepository documentsRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;

    @Value("${file.upload-dir:C:/Users/Stagiaire_APS/Desktop/sirh-backend/uploads}")
    private String uploadDir;

    // ==========================================
    // LOGIQUE METIER : GESTION DES CONTRATS
    // ==========================================

    public Contrat creerContrat(Contrat contrat, String employeeId, String creator) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        if (contratRepository.existsByEmployee_IdAndStatut(employeeId, "ACTIF") && "ACTIF".equals(contrat.getStatut())) {
            throw new RuntimeException("Cet employé possède déjà un contrat actif. Veuillez d'abord le résilier ou l'archiver.");
        }

        contrat.setEmployee(emp);
        contrat.setCreatedAt(LocalDate.now());
        contrat.setCreatedBy(creator);

        return contratRepository.save(contrat);
    }

    public Contrat modifierContrat(String id, Contrat updatedData, String updater) {
        Contrat existing = contratRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat introuvable"));

        existing.setTypeContrat(updatedData.getTypeContrat());
        existing.setDateDebut(updatedData.getDateDebut());
        existing.setDateFin(updatedData.getDateFin());
        existing.setStatut(updatedData.getStatut());
        if (updatedData.getImageUrls() != null) {
            existing.setImageUrls(updatedData.getImageUrls());
        }
        existing.setUpdatedAt(LocalDate.now());
        existing.setUpdatedBy(updater);

        return contratRepository.save(existing);
    }

    public void resilierContrat(String id, String updater) {
        Contrat existing = contratRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat introuvable"));
        existing.setStatut("EXPIRE");
        existing.setUpdatedAt(LocalDate.now());
        existing.setUpdatedBy(updater);
        contratRepository.save(existing);
    }

    public void archiverContrat(String id, String updater) {
        Contrat existing = contratRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat introuvable"));
        existing.setStatut("ARCHIVE");
        existing.setUpdatedAt(LocalDate.now());
        existing.setUpdatedBy(updater);
        contratRepository.save(existing);
    }

    public List<Contrat> getAllContrats() {
        return contratRepository.findAll();
    }

    public Contrat getContratById(String id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat introuvable"));
    }

    // ==========================================
    // LOGIQUE METIER : GESTION DES DOCUMENTS
    // ==========================================

    public Documents ajouterDocumentEmploye(String employeeId, String name, String typeDocument,
                                            List<String> imageUrls, List<MultipartFile> files, String creator) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        List<String> allUrls = new ArrayList<>();
        if (imageUrls != null) allUrls.addAll(imageUrls);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String savedUrl = saveFile(file, employeeId, typeDocument);
                allUrls.add(savedUrl);
            }
        }

        Documents doc = new Documents();
        doc.setName(name);
        doc.setTypeDocument(typeDocument);
        doc.setImageUrls(allUrls);
        doc.setDateUpload(LocalDate.now());
        doc.setEmployee(emp);
        doc.setCreatedAt(LocalDate.now());
        doc.setCreatedBy(creator);

        Documents saved = documentsRepository.save(doc);

        // 🔔 Notification DOCUMENT_UPLOADED
        publishDocumentUploadEvent(emp, doc, creator);

        return saved;
    }

    public Documents ajouterDocumentAContrat(String contratId, String name, String typeDocument,
                                             List<String> imageUrls, List<MultipartFile> files, String creator) {
        Contrat ctr = contratRepository.findById(contratId)
                .orElseThrow(() -> new RuntimeException("Contrat introuvable"));

        if (ctr.getEmployee() == null) {
            throw new RuntimeException("Ce contrat n'est lié à aucun employé. Impossible d'ajouter un document.");
        }

        Employee emp = ctr.getEmployee();

        List<String> allUrls = new ArrayList<>();
        if (imageUrls != null) allUrls.addAll(imageUrls);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String savedUrl = saveFile(file, emp.getId(), typeDocument);
                allUrls.add(savedUrl);
            }
        }

        Documents doc = new Documents();
        doc.setName(name);
        doc.setTypeDocument(typeDocument);
        doc.setImageUrls(allUrls);
        doc.setDateUpload(LocalDate.now());
        doc.setContrat(ctr);
        doc.setEmployee(emp);
        doc.setCreatedAt(LocalDate.now());
        doc.setCreatedBy(creator);

        Documents savedDoc = documentsRepository.save(doc);

        if (!allUrls.isEmpty()) {
            if (ctr.getImageUrls() == null) {
                ctr.setImageUrls(new ArrayList<>());
            }
            ctr.getImageUrls().addAll(allUrls);
            contratRepository.save(ctr);
        }

        // 🔔 Notification DOCUMENT_UPLOADED
        publishDocumentUploadEvent(emp, doc, creator);

        return savedDoc;
    }

    private String saveFile(MultipartFile file, String employeeId, String typeDocument) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            String subDir = switch (typeDocument.toUpperCase()) {
                case "CNI" -> "cni";
                case "PHOTO" -> "photos";
                case "CERTIFICAT" -> "certificats";
                case "DIPLOME" -> "diplomes";
                case "CONTRAT_SIGNE" -> "contrats";
                default -> "documents";
            };

            Path uploadPath = Paths.get(uploadDir).resolve(subDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            return "/uploads/" + subDir + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier", e);
        }
    }

    // ==========================================
    // RÉCUPÉRATION DES DOCUMENTS
    // ==========================================

    public List<Documents> getDocumentsByEmployee(String employeeId) {
        log.info("📄 Récupération des documents pour l'employé ID: {}", employeeId);
        try {
            if (employeeId == null || employeeId.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return documentsRepository.findByEmployee_Id(employeeId);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Documents> getDocumentsByContrat(String contratId) {
        log.info("📄 Récupération des documents pour le contrat ID: {}", contratId);
        try {
            if (contratId == null || contratId.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return documentsRepository.findByContrat_Id(contratId);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Documents> getDocumentsByType(String typeDocument) {
        log.info("📄 Récupération des documents de type: {}", typeDocument);
        try {
            if (typeDocument == null || typeDocument.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return documentsRepository.findByTypeDocument(typeDocument);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Documents> getDocumentsByEmployeeAndType(String employeeId, String typeDocument) {
        log.info("📄 Récupération des documents de type {} pour l'employé ID: {}", typeDocument, employeeId);
        try {
            if (employeeId == null || employeeId.trim().isEmpty() || typeDocument == null || typeDocument.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return documentsRepository.findByEmployee_IdAndTypeDocument(employeeId, typeDocument);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Documents getDocumentById(String id) {
        return documentsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));
    }

    public List<Documents> getAllDocuments() {
        log.info("📄 Récupération de tous les documents");
        try {
            return documentsRepository.findAll();
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de tous les documents: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getDocumentUrlsByEmployee(String employeeId) {
        log.info("📄 Récupération des URLs des documents pour l'employé ID: {}", employeeId);
        try {
            List<Documents> docs = documentsRepository.findByEmployee_Id(employeeId);
            return docs.stream()
                    .map(doc -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", doc.getId());
                        info.put("name", doc.getName());
                        info.put("type", doc.getTypeDocument());
                        info.put("urls", doc.getImageUrls());
                        info.put("uploadDate", doc.getDateUpload());
                        info.put("createdAt", doc.getCreatedAt());
                        info.put("createdBy", doc.getCreatedBy());
                        return info;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des URLs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public void supprimerDocument(String id) {
        log.info("🗑️ Suppression du document ID: {}", id);
        try {
            if (id == null || id.trim().isEmpty()) {
                log.warn("⚠️ ID document null ou vide");
                return;
            }
            documentsRepository.deleteById(id);
            log.info("✅ Document {} supprimé avec succès", id);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du document {}: {}", id, e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression du document: " + e.getMessage());
        }
    }

    public Documents updateDocument(Documents document) {
        log.info("📝 Mise à jour du document ID: {}", document.getId());
        try {
            return documentsRepository.save(document);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du document: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour du document: " + e.getMessage());
        }
    }

    // ==========================================
    // MÉTHODE DE PUBLICATION DES NOTIFICATIONS
    // ==========================================

    private void publishDocumentUploadEvent(Employee employee, Documents doc, String creator) {
        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employee.getId());
        data.put("companyId", employee.getEntrepriseId());
        data.put("departmentId", employee.getDepartementId());
        data.put("entityId", doc.getId());
        data.put("entityType", "DOCUMENT");
        data.put("actionUrl", "/documents/" + doc.getId());
        data.put("metadata", Map.of(
                "name", doc.getName(),
                "typeDocument", doc.getTypeDocument()
        ));
        // On ajoute le triggeredBy (creator) et on ajoute le déclencheur pour le résolver
        // Le résolver ajoutera le creator comme destinataire si besoin.
        notificationPublisher.publish(NotificationEvent.DOCUMENT_UPLOADED, data, creator);
    }
}