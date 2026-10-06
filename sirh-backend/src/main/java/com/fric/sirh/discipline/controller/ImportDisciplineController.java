package com.fric.sirh.discipline.controller;

import com.fric.sirh.discipline.dto.ImportDemandeExplicationDTO;
import com.fric.sirh.discipline.service.ImportDisciplineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/discipline/import")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ImportDisciplineController {

    private final ImportDisciplineService importService;

    @PostMapping("/explanations")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_CREATE')")
    public ResponseEntity<Map<String, Object>> importerDemande(@RequestBody ImportDemandeExplicationDTO dto) {
        log.info("📥 Import d'une demande d'explication: {}", dto.getNumeroOriginal());

        ImportDisciplineService.ImportResult result = importService.importerDemandeExplication(dto);

        Map<String, Object> response = new HashMap<>();

        if (result.isSuccess()) {
            response.put("success", true);
            response.put("message", "Demande importée avec succès");
            response.put("numero", result.getNumeroOriginal());
            response.put("id", result.getDemandeId());
            return ResponseEntity.ok(response);
        } else if (result.isDuplicate()) {
            response.put("success", false);
            response.put("message", "La demande existe déjà");
            response.put("numero", result.getNumeroOriginal());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else {
            response.put("success", false);
            response.put("message", "Erreur lors de l'import");
            response.put("error", result.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/explanations/mass")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_CREATE')")
    public ResponseEntity<Map<String, Object>> importerDemandesEnMasse(@RequestBody List<ImportDemandeExplicationDTO> dtos) {
        log.info("📥 Import en masse de {} demandes", dtos.size());

        List<ImportDisciplineService.ImportResult> results = importService.importerDemandesEnMasse(dtos);

        long success = results.stream().filter(ImportDisciplineService.ImportResult::isSuccess).count();
        long duplicates = results.stream().filter(ImportDisciplineService.ImportResult::isDuplicate).count();
        long failures = results.stream().filter(ImportDisciplineService.ImportResult::isFailure).count();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("total", results.size());
        response.put("successCount", success);
        response.put("duplicateCount", duplicates);
        response.put("failureCount", failures);

        List<Map<String, Object>> details = results.stream()
                .map(r -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("numero", r.getNumeroOriginal());
                    detail.put("status", r.isSuccess() ? "SUCCESS" :
                            r.isDuplicate() ? "DUPLICATE" : "FAILURE");
                    detail.put("message", r.isSuccess() ? "Importé" : r.getErrorMessage());
                    if (r.getDemandeId() != null) {
                        detail.put("id", r.getDemandeId());
                    }
                    return detail;
                })
                .toList();
        response.put("details", details);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/explanations/csv")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_CREATE')")
    public ResponseEntity<Map<String, Object>> importerDepuisCSV(@RequestParam("file") MultipartFile file) {
        log.info("📥 Import depuis un fichier CSV: {}", file.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();

        try {
            // TODO: Implémenter la lecture CSV
            response.put("success", true);
            response.put("message", "Fonctionnalité CSV à implémenter");
            response.put("filename", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'import CSV: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Erreur lors de l'import");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}