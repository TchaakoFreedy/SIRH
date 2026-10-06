package com.fric.sirh.service;

import com.fric.sirh.model.BulletinPaie;
import com.fric.sirh.model.BulletinStatus;
import com.fric.sirh.model.Employee;
import com.fric.sirh.repository.BulletinPaieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BulletinStorageService {

    private final BulletinPaieRepository bulletinRepository;
    private final BulletinNotificationService notificationService;
    private final FileStorageService fileStorageService;

    private static final String PAYSLIP_DIR = "payslip";

    public String storePdf(MultipartFile file) throws IOException {
        log.info("📄 Stockage du PDF : {}", file.getOriginalFilename());
        String url = fileStorageService.storeFile(file, PAYSLIP_DIR);
        log.info("✅ PDF stocké avec URL : {}", url);
        return url;
    }

    public String storeImage(BufferedImage image, String fileName) throws IOException {
        log.info("🖼️ Stockage de l'image : {}", fileName);
        String url = fileStorageService.storeImage(image, PAYSLIP_DIR, fileName);
        log.info("✅ Image stockée avec URL : {}", url);
        return url;
    }

    public Resource getResource(String fileUrl) {
        log.info("📂 Récupération du fichier avec URL : '{}'", fileUrl);
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("L'URL du fichier ne peut pas être null ou vide.");
        }

        try {
            // Nettoyer l'URL
            String cleanUrl = fileUrl;
            if (cleanUrl.startsWith("/uploads/")) {
                cleanUrl = cleanUrl.substring(9);
                log.info("📂 URL nettoyée (suppression de /uploads/) : '{}'", cleanUrl);
            }
            if (cleanUrl.startsWith("/")) {
                cleanUrl = cleanUrl.substring(1);
                log.info("📂 URL nettoyée (suppression du / initial) : '{}'", cleanUrl);
            }

            log.info("📂 URL finale : '{}'", cleanUrl);

            Path filePath = fileStorageService.getPathFromUrl(cleanUrl);
            log.info("📂 Chemin complet : '{}'", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                log.error("❌ Fichier introuvable : {}", filePath.toAbsolutePath());

                // Tentative avec l'URL originale
                log.info("🔄 Tentative avec l'URL originale : '{}'", fileUrl);
                Path originalPath = fileStorageService.getPathFromUrl(fileUrl);
                if (Files.exists(originalPath)) {
                    log.info("✅ Fichier trouvé avec l'URL originale");
                    Resource resource = new UrlResource(originalPath.toUri());
                    if (resource.exists() && resource.isReadable()) {
                        return resource;
                    }
                }

                throw new RuntimeException("Fichier introuvable avec URL : " + fileUrl);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.info("✅ Fichier trouvé : {}", resource.getFilename());
                return resource;
            } else {
                throw new RuntimeException("Fichier inaccessible avec URL : " + fileUrl);
            }
        } catch (MalformedURLException e) {
            log.error("❌ URL invalide : {}", fileUrl, e);
            throw new RuntimeException("URL invalide : " + fileUrl);
        } catch (IOException e) {
            log.error("❌ Erreur lors de la récupération du fichier : {}", fileUrl, e);
            throw new RuntimeException("Erreur lors de la récupération du fichier : " + fileUrl);
        }
    }

    public BulletinPaie saveOrUpdateBulletin(Employee employee, Map<String, String> extractedData,
                                             List<String> imageUrls, String pdfUrl, String uploadedBy,
                                             List<String> errors) {
        String matricule = extractedData.get("matricule");

        // 🔥 CORRECTION : Récupérer le nom depuis l'objet Employee
        String nom = null;
        if (employee != null) {
            // Utiliser le nom complet depuis l'employé
            nom = employee.getPrenom() + " " + employee.getNom();
            log.info("📝 Nom récupéré depuis la base de données: {}", nom);
        }

        // Fallback si l'employé est null ou n'a pas de nom
        if (nom == null || nom.trim().isEmpty() || "null null".equals(nom.trim())) {
            nom = "Employé " + matricule;
            log.warn("⚠️ Aucun nom trouvé en base, utilisation de la valeur par défaut: {}", nom);
        }

        String periode = extractedData.get("periode");

        int month = extractMonth(periode);
        int year = extractYear(periode);

        if (month == 0 || year == 0) {
            LocalDateTime now = LocalDateTime.now();
            month = now.getMonthValue();
            year = now.getYear();
            log.warn("⚠️ Période non extraite, utilisation de la date actuelle: {}/{}", month, year);
        }

        log.info("📝 Enregistrement du bulletin pour matricule: {}, nom: {}, mois: {}, année: {}",
                matricule, nom, month, year);

        BulletinPaie existing = bulletinRepository.findByEmployeeMatriculeAndMonthAndYear(matricule, month, year)
                .orElse(null);

        if (existing != null) {
            log.info("🔄 Mise à jour du bulletin existant pour {} - {}/{}", matricule, month, year);
            for (String url : imageUrls) {
                if (!existing.getImageUrls().contains(url)) {
                    existing.getImageUrls().add(url);
                }
            }
            // 🔥 Mettre à jour le nom si l'employé a changé
            if (nom != null) {
                existing.setEmployeeFullName(nom);
            }
            if (employee != null) {
                existing.setEmployee(employee);
            }
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(uploadedBy);
            existing.setStatus(errors.isEmpty() ? BulletinStatus.SUCCESS : BulletinStatus.PARTIAL_SUCCESS);
            existing.getImportErrors().addAll(errors);
            BulletinPaie saved = bulletinRepository.save(existing);
            log.info("✅ Bulletin mis à jour pour {} - {}/{}", matricule, month, year);
            notificationService.notifyEmployee(employee, saved);
            return saved;
        } else {
            log.info("🆕 Création d'un nouveau bulletin pour {} - {}/{}", matricule, month, year);
            BulletinPaie bulletin = new BulletinPaie();
            bulletin.setEmployeeMatricule(matricule);
            // 🔥 Utiliser le nom de l'employé depuis la base de données
            bulletin.setEmployeeFullName(nom);
            bulletin.setMonth(month);
            bulletin.setYear(year);
            bulletin.setPeriod(periode != null ? periode : String.format("%02d/%d", month, year));
            bulletin.setGrossSalary(parseBigDecimal(extractedData.get("brut")));
            bulletin.setNetSalary(parseBigDecimal(extractedData.get("net")));
            bulletin.setDeductions(parseBigDecimal(extractedData.get("deductions")));
            bulletin.setPdfFileUrl(pdfUrl);

            Set<String> uniqueUrlsSet = new HashSet<>(imageUrls);
            bulletin.setImageUrls(new ArrayList<>(uniqueUrlsSet));

            bulletin.setUploadedFileName(uploadedBy);
            bulletin.setEmployee(employee);
            bulletin.setCreatedBy(uploadedBy);
            bulletin.setCreatedAt(LocalDateTime.now());
            bulletin.setStatus(errors.isEmpty() ? BulletinStatus.SUCCESS : BulletinStatus.PARTIAL_SUCCESS);
            bulletin.setImportErrors(errors);

            BulletinPaie saved = bulletinRepository.save(bulletin);
            log.info("✅ Nouveau bulletin créé pour {} - {}/{} avec nom: {}", matricule, month, year, nom);
            notificationService.notifyEmployee(employee, saved);
            return saved;
        }
    }

    private int extractMonth(String periode) {
        try {
            if (periode == null) return 0;
            String[] parts = periode.split("[/\\s]+");
            String monthStr = parts[0].trim();

            Map<String, Integer> moisMap = new HashMap<>();
            moisMap.put("Janvier", 1);
            moisMap.put("Fevrier", 2);
            moisMap.put("Mars", 3);
            moisMap.put("Avril", 4);
            moisMap.put("Mai", 5);
            moisMap.put("Juin", 6);
            moisMap.put("Juillet", 7);
            moisMap.put("Aout", 8);
            moisMap.put("Septembre", 9);
            moisMap.put("Octobre", 10);
            moisMap.put("Novembre", 11);
            moisMap.put("Decembre", 12);

            if (moisMap.containsKey(monthStr)) return moisMap.get(monthStr);
            return Integer.parseInt(monthStr);
        } catch (Exception e) {
            log.warn("⚠️ Impossible d'extraire le mois de la periode : '{}'", periode);
            return 0;
        }
    }

    private int extractYear(String periode) {
        try {
            if (periode == null) return 0;
            String[] parts = periode.split("[/\\s]+");
            if (parts.length >= 2) {
                String yearStr = parts[1].trim();
                if (yearStr.length() == 2) {
                    yearStr = "20" + yearStr;
                }
                return Integer.parseInt(yearStr);
            }
            return 0;
        } catch (Exception e) {
            log.warn("⚠️ Impossible d'extraire l'année de la periode : '{}'", periode);
            return 0;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = value.replaceAll("\\s+", "").replace(",", ".");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("⚠️ Impossible de parser le nombre : '{}'", value);
            return BigDecimal.ZERO;
        }
    }

    public Resource getZipForImages(List<String> imageUrls, String zipName) {
        log.info("📦 Création du ZIP pour {} images", imageUrls.size());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                try {
                    Resource img = getResource(url);
                    String fileName = "page-" + (i + 1) + ".png";
                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    try (InputStream is = img.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                    log.info("✅ Ajout de l'image {} au ZIP", url);
                } catch (Exception e) {
                    log.error("❌ Erreur lors de l'ajout de l'image {} au ZIP", url, e);
                }
            }
            zos.finish();
            byte[] zipBytes = baos.toByteArray();
            log.info("✅ ZIP créé avec succès, taille : {} octets", zipBytes.length);
            return new InputStreamResource(new ByteArrayInputStream(zipBytes));
        } catch (IOException e) {
            log.error("❌ Erreur lors de la création du ZIP", e);
            throw new RuntimeException("Erreur création ZIP", e);
        }
    }
}