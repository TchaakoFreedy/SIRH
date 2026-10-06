package com.fric.sirh.controller;

import com.fric.sirh.model.Entreprise;
import com.fric.sirh.service.EntrepriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EntrepriseController {

    private final EntrepriseService service;

    @PostMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Entreprise> create(@RequestBody Entreprise entreprise) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(entreprise));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COMPANY_VIEW_ALL')")
    public ResponseEntity<List<Entreprise>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public ResponseEntity<Entreprise> getById(@PathVariable String id) {
        Entreprise entreprise = service.getById(id);
        if (entreprise == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entreprise);
    }

    // ✅ NOUVEAU: Récupérer l'entreprise par ID employé
    @GetMapping("/by-employee/{employeeId}")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public ResponseEntity<Entreprise> getByEmployeeId(@PathVariable String employeeId) {
        log.info("🔍 GET /by-employee/{} - Récupération de l'entreprise pour l'employé", employeeId);
        try {
            Entreprise entreprise = service.getEntrepriseByEmployeeId(employeeId);
            if (entreprise == null) {
                log.warn("⚠️ Aucune entreprise trouvée pour l'employé ID: {}", employeeId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entreprise);
        } catch (RuntimeException e) {
            log.error("❌ Erreur lors de la récupération de l'entreprise: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ NOUVEAU: Récupérer l'entreprise de l'employé connecté
    @GetMapping("/my-entreprise")
    @PreAuthorize("hasAuthority('COMPANY_VIEW')")
    public ResponseEntity<Entreprise> getMyEntreprise(@RequestParam String employeeId) {
        log.info("🔍 GET /my-entreprise?employeeId={} - Récupération de l'entreprise pour l'employé connecté", employeeId);
        try {
            Entreprise entreprise = service.getEntrepriseByEmployeeId(employeeId);
            if (entreprise == null) {
                log.warn("⚠️ Aucune entreprise trouvée pour l'employé connecté ID: {}", employeeId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entreprise);
        } catch (RuntimeException e) {
            log.error("❌ Erreur lors de la récupération de l'entreprise: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Entreprise> update(
            @PathVariable String id,
            @RequestBody Entreprise entreprise) {
        Entreprise updated = service.update(id, entreprise);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/suspendre")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Entreprise> suspendre(@PathVariable String id) {
        Entreprise entreprise = service.suspendre(id);
        if (entreprise == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entreprise);
    }

    @PutMapping("/{id}/reactiver")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Entreprise> reactiver(@PathVariable String id) {
        Entreprise entreprise = service.reactiver(id);
        if (entreprise == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entreprise);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}