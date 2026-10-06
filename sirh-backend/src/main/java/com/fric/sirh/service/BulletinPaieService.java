package com.fric.sirh.service;

import com.fric.sirh.dto.BulletinPaieDTO;
import com.fric.sirh.dto.BulletinPaieUploadResponse;
import com.fric.sirh.model.BulletinPaie;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.BulletinPaieRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.service.pdf.AfricBulletinParserService;
import com.fric.sirh.service.pdf.PdfImageService;
import com.fric.sirh.service.pdf.PdfTextExtractorService;
import com.fric.sirh.util.RegexExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulletinPaieService {

    private final BulletinPaieRepository bulletinRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final BulletinStorageService storageService;
    private final BulletinMatchingService matchingService;
    private final PdfTextExtractorService textExtractor;
    private final PdfImageService imageService;
    private final AfricBulletinParserService africParser;
    private final RegexExtractor regexExtractor;
    private final FileStorageService fileStorageService;

    @Async("payrollExecutor")
    public CompletableFuture<BulletinPaieUploadResponse> processPaySlipUploadAsync(MultipartFile file, String userId) {
        return CompletableFuture.completedFuture(processPaySlipUpload(file, userId));
    }

    @Transactional
    public BulletinPaieUploadResponse processPaySlipUpload(MultipartFile file, String userId) {
        BulletinPaieUploadResponse response = new BulletinPaieUploadResponse();
        List<String> globalErrors = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        String pdfUrl = null;
        try {
            pdfUrl = storageService.storePdf(file);
            response.setFileId(pdfUrl);
            log.info("PDF stocke avec URL: {}", pdfUrl);
        } catch (Exception e) {
            globalErrors.add("Erreur stockage PDF : " + e.getMessage());
            response.setErrors(globalErrors);
            response.setStatus("FAILED");
            response.setMessage("Erreur lors du stockage du PDF");
            return response;
        }

        byte[] fileBytes;
        try (InputStream is = file.getInputStream()) {
            fileBytes = is.readAllBytes();
        } catch (IOException e) {
            globalErrors.add("Erreur lecture fichier : " + e.getMessage());
            response.setErrors(globalErrors);
            response.setStatus("FAILED");
            response.setMessage("Erreur lors de la lecture du fichier");
            return response;
        }

        try (PDDocument document = Loader.loadPDF(fileBytes)) {

            int totalPages = document.getNumberOfPages();
            response.setTotalPages(totalPages);
            response.setPageCount(totalPages);

            Map<String, BulletinProcessingContext> contextMap = new HashMap<>();
            int failure = 0;
            int success = 0;
            int textPages = 0;
            int employeesMatched = 0;
            int employeesNotMatched = 0;
            List<String> imageIds = new ArrayList<>();

            for (int pageIdx = 1; pageIdx <= totalPages; pageIdx++) {
                try {
                    String pageText = null;

                    log.info("Page {} - Extraction du texte PDF...", pageIdx);

                    try {
                        pageText = textExtractor.extractLinesFromPage(document, pageIdx);

                        if (pageText != null && !pageText.isBlank() && pageText.length() > 20) {
                            textPages++;
                            log.info("Page {} - Texte extrait avec PDFBox ({} caracteres).",
                                    pageIdx, pageText.length());
                        } else {
                            log.warn("Page {} - Texte vide ou trop court. Tentative avec extraction standard.", pageIdx);

                            String standardText = textExtractor.extractTextFromPage(document, pageIdx);
                            if (standardText != null && !standardText.isBlank() && standardText.length() > 20) {
                                pageText = standardText;
                                textPages++;
                                log.info("Page {} - Texte extrait avec extraction standard ({} caracteres).",
                                        pageIdx, pageText.length());
                            } else {
                                log.warn("Page {} - Aucun texte extrait.", pageIdx);
                                pageText = "AUCUN_TEXTE_DETECTE";
                            }
                        }

                    } catch (Exception e) {
                        log.error("Page {} - Erreur extraction texte: {}", pageIdx, e.getMessage());
                        pageText = "ERREUR_EXTRACTION: " + e.getMessage();
                    }

                    if (pageText == null || pageText.isBlank() || pageText.length() < 20) {
                        pageText = "AUCUN_TEXTE_DETECTE";
                        log.warn("Page {} - Aucun texte detecte", pageIdx);
                    }

                    Map<String, String> extracted = africParser.parseBulletinData(pageText);

                    if (extracted.isEmpty() || !extracted.containsKey("matricule") ||
                            extracted.get("matricule").isEmpty() ||
                            extracted.get("matricule").equalsIgnoreCase("null")) {
                        log.warn("Page {} - AfricParser n'a pas trouve de donnees, utilisation de RegexExtractor", pageIdx);
                        extracted = regexExtractor.extract(pageText);
                    }

                    log.info("Page {} - Donnees extraites: {}", pageIdx, extracted);

                    if (extracted.isEmpty() || !extracted.containsKey("matricule") ||
                            extracted.get("matricule").isEmpty() ||
                            extracted.get("matricule").equalsIgnoreCase("null")) {
                        globalErrors.add("Page " + pageIdx + " : aucune donnee valide (matricule manquant)");
                        failure++;
                        continue;
                    }

                    Employee employee = matchingService.matchEmployee(extracted);
                    if (employee == null) {
                        globalErrors.add("Page " + pageIdx + " : employe introuvable pour matricule " +
                                extracted.get("matricule"));
                        employeesNotMatched++;
                        failure++;
                        continue;
                    }
                    employeesMatched++;

                    String imageUrl = null;
                    try {
                        BufferedImage pageImage = imageService.renderPageToImage(document, pageIdx, 150);
                        imageUrl = storageService.storeImage(pageImage, "page-" + pageIdx + ".png");
                        imageIds.add(imageUrl);
                        log.info("Page {} - Image generee avec URL: {}", pageIdx, imageUrl);
                    } catch (Exception e) {
                        log.warn("Page {} - Impossible de generer l'image: {}", pageIdx, e.getMessage());
                    }

                    String matricule = extracted.get("matricule");
                    BulletinProcessingContext ctx = contextMap.get(matricule);
                    if (ctx == null) {
                        ctx = new BulletinProcessingContext(employee, extracted);
                        contextMap.put(matricule, ctx);
                    }

                    if (imageUrl != null && !ctx.imageUrls.contains(imageUrl)) {
                        ctx.imageUrls.add(imageUrl);
                    }

                } catch (Exception e) {
                    globalErrors.add("Page " + pageIdx + " : " + e.getMessage());
                    failure++;
                    log.error("Erreur page {}", pageIdx, e);
                }
            }

            int created = 0;
            for (BulletinProcessingContext ctx : contextMap.values()) {
                try {
                    List<String> errors = new ArrayList<>();
                    BulletinPaie saved = storageService.saveOrUpdateBulletin(
                            ctx.employee,
                            ctx.extractedData,
                            ctx.imageUrls,
                            pdfUrl,
                            userId,
                            errors
                    );
                    created++;
                    success++;
                    log.info("Bulletin enregistre pour matricule: {}", ctx.employee.getMatriculeInterne());
                } catch (Exception e) {
                    globalErrors.add("Erreur enregistrement pour " + ctx.employee.getMatriculeInterne() + " : " + e.getMessage());
                    log.error("Erreur enregistrement", e);
                }
            }

            response.setTotalEmployeesProcessed(contextMap.size());
            response.setSuccessCount(success);
            response.setFailureCount(failure);
            response.setErrors(globalErrors);
            response.setImageIds(imageIds);
            response.setTextPages(textPages);
            response.setEmployeesMatched(employeesMatched);
            response.setEmployeesNotMatched(employeesNotMatched);
            response.setCreatedPayrolls(created);

            long endTime = System.currentTimeMillis();
            response.setProcessingTime(String.format("%d ms", endTime - startTime));

            if (failure > 0 && success > 0) {
                response.setStatus("PARTIAL_SUCCESS");
                response.setMessage("Import termine avec des erreurs");
            } else if (failure > 0 && success == 0) {
                response.setStatus("FAILED");
                response.setMessage("Import echoue");
            } else {
                response.setStatus("SUCCESS");
                response.setMessage("Import reussi");
            }

            log.info("Resume de l'import: {} bulletins traites, {} reussis, {} echoues",
                    contextMap.size(), success, failure);

        } catch (Exception e) {
            globalErrors.add("Erreur globale : " + e.getMessage());
            log.error("Echec de l'import", e);
            response.setErrors(globalErrors);
            response.setStatus("FAILED");
            response.setMessage("Erreur lors du traitement du fichier: " + e.getMessage());
        }
        return response;
    }

    public List<BulletinPaieDTO> getAllBulletins() {
        log.info("Recuperation de tous les bulletins");
        List<BulletinPaie> bulletins = bulletinRepository.findAll();
        log.info("{} bulletins trouves", bulletins.size());
        return bulletins.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public BulletinPaieDTO getBulletinById(String id) {
        log.info("Recuperation du bulletin avec ID: {}", id);
        return toDTO(bulletinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + id)));
    }

    @Transactional
    public BulletinPaieDTO updateBulletin(String id, BulletinPaieDTO dto, String userId) {
        log.info("Mise a jour du bulletin {} par {}", id, userId);

        BulletinPaie bulletin = bulletinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + id));

        if (dto.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employe non trouve avec ID: " + dto.getEmployeeId()));
            bulletin.setEmployee(emp);
        }
        if (dto.getGrossSalary() != null) bulletin.setGrossSalary(dto.getGrossSalary());
        if (dto.getNetSalary() != null) bulletin.setNetSalary(dto.getNetSalary());
        if (dto.getDeductions() != null) bulletin.setDeductions(dto.getDeductions());
        if (dto.getMonth() > 0) bulletin.setMonth(dto.getMonth());
        if (dto.getYear() > 0) bulletin.setYear(dto.getYear());
        if (dto.getPeriod() != null) bulletin.setPeriod(dto.getPeriod());

        bulletin.setUpdatedAt(LocalDateTime.now());
        bulletin.setUpdatedBy(userId);

        BulletinPaie saved = bulletinRepository.save(bulletin);
        log.info("Bulletin {} mis a jour avec succes", id);
        return toDTO(saved);
    }

    @Transactional
    public void deleteBulletin(String id, String userId) {
        log.info("Suppression du bulletin {} par {}", id, userId);

        BulletinPaie bulletin = bulletinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + id));

        if (bulletin.getPdfFileUrl() != null) {
            try {
                fileStorageService.deleteFile(bulletin.getPdfFileUrl());
                log.info("PDF supprime: {}", bulletin.getPdfFileUrl());
            } catch (Exception e) {
                log.error("Erreur lors de la suppression du PDF: {}", e.getMessage());
            }
        }

        if (bulletin.getImageUrls() != null) {
            for (String imageUrl : bulletin.getImageUrls()) {
                try {
                    fileStorageService.deleteFile(imageUrl);
                    log.info("Image supprimee: {}", imageUrl);
                } catch (Exception e) {
                    log.error("Erreur lors de la suppression de l'image: {}", e.getMessage());
                }
            }
        }

        bulletinRepository.delete(bulletin);
        log.info("Bulletin {} supprime avec succes", id);
    }

    public Resource downloadBulletin(String id) {
        log.info("Telechargement du bulletin {}", id);

        BulletinPaie bulletin = bulletinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + id));

        if (bulletin.getPdfFileUrl() != null) {
            log.info("Recuperation du PDF: {}", bulletin.getPdfFileUrl());
            return storageService.getResource(bulletin.getPdfFileUrl());
        }
        throw new RuntimeException("Aucun PDF associe au bulletin " + id);
    }

    public List<String> getPageIds(String bulletinId) {
        BulletinPaie bulletin = bulletinRepository.findById(bulletinId)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + bulletinId));
        return bulletin.getImageUrls() != null ? bulletin.getImageUrls() : Collections.emptyList();
    }

    public Resource getPageImage(String bulletinId, int pageNumber) {
        log.info("Recuperation de la page {} du bulletin {}", pageNumber, bulletinId);

        BulletinPaie bulletin = bulletinRepository.findById(bulletinId)
                .orElseThrow(() -> new RuntimeException("Bulletin non trouve avec ID: " + bulletinId));

        List<String> imageUrls = bulletin.getImageUrls();
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new RuntimeException("Aucune image disponible pour ce bulletin");
        }

        if (pageNumber < 1 || pageNumber > imageUrls.size()) {
            throw new RuntimeException("Numero de page invalide: " + pageNumber +
                    ". Le bulletin contient " + imageUrls.size() + " pages");
        }

        String imageUrl = imageUrls.get(pageNumber - 1);
        log.info("Recuperation de l'image: {}", imageUrl);
        return storageService.getResource(imageUrl);
    }

    public Resource getBulletinZip(String bulletinId) {
        throw new RuntimeException("Le telechargement ZIP n'est plus supporte. Veuillez utiliser le telechargement PDF ou les images individuelles.");
    }

    public Resource getImageResource(String imageId) {
        log.info("Recuperation de l'image: {}", imageId);
        return storageService.getResource(imageId);
    }

    public List<BulletinPaieDTO> getBulletinsForCurrentUser(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("Recuperation des bulletins pour l'utilisateur: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve avec email: " + userEmail));

        String employeeId = user.getEmployeeId();
        if (employeeId == null) {
            log.warn("L'utilisateur {} n'a pas d'employe associe", userEmail);
            return Collections.emptyList();
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe non trouve avec ID: " + employeeId));

        String matricule = employee.getMatriculeInterne();
        if (matricule == null) {
            log.warn("L'employe {} n'a pas de matricule interne", employeeId);
            return Collections.emptyList();
        }

        List<BulletinPaie> bulletins = bulletinRepository.findByEmployeeMatricule(matricule);
        log.info("{} bulletins trouves pour le matricule {}", bulletins.size(), matricule);

        return bulletins.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private BulletinPaieDTO toDTO(BulletinPaie bulletin) {
        if (bulletin == null) return null;

        BulletinPaieDTO dto = new BulletinPaieDTO();
        dto.setId(bulletin.getId());
        dto.setEmployeeId(bulletin.getEmployee() != null ? bulletin.getEmployee().getId() : null);
        dto.setEmployeeMatricule(bulletin.getEmployeeMatricule());
        dto.setEmployeeFullName(bulletin.getEmployeeFullName());
        dto.setMonth(bulletin.getMonth());
        dto.setYear(bulletin.getYear());
        dto.setPeriod(bulletin.getPeriod());
        dto.setGrossSalary(bulletin.getGrossSalary());
        dto.setNetSalary(bulletin.getNetSalary());
        dto.setDeductions(bulletin.getDeductions());
        dto.setPdfFileUrl(bulletin.getPdfFileUrl());
        dto.setImageUrls(bulletin.getImageUrls());
        dto.setUploadedFileName(bulletin.getUploadedFileName());
        dto.setCreatedAt(bulletin.getCreatedAt());
        dto.setCreatedBy(bulletin.getCreatedBy());
        dto.setUpdatedAt(bulletin.getUpdatedAt());
        dto.setUpdatedBy(bulletin.getUpdatedBy());
        dto.setStatus(bulletin.getStatus() != null ? bulletin.getStatus().name() : null);
        dto.setImportErrors(bulletin.getImportErrors());
        return dto;
    }

    private static class BulletinProcessingContext {
        Employee employee;
        Map<String, String> extractedData;
        List<String> imageUrls = new ArrayList<>();

        BulletinProcessingContext(Employee employee, Map<String, String> extractedData) {
            this.employee = employee;
            this.extractedData = extractedData;
        }
    }
}