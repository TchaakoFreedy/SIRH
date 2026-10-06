package com.fric.sirh.controller;

import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.ConfigurationConge;
import com.fric.sirh.service.ConfigurationCongeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/configurations/conges")
@RequiredArgsConstructor
public class ConfigurationCongeController {

    private final ConfigurationCongeService service;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> create(@RequestBody ConfigurationConge config) {
        log.info("📥 POST /api/configurations/conges - Corps reçu : {}", config);
        try {
            ConfigurationConge created = service.create(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("⚠️ Erreur de validation : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne : " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<List<ConfigurationConge>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return service.getById(id)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResourceNotFoundException("ConfigurationConge", id));
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration introuvable avec ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/global")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> getGlobal() {
        try {
            ConfigurationConge config = service.getGlobalConfiguration();
            return ResponseEntity.ok(config);
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration globale non trouvée");
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH') or hasAuthority('LEAVE_VIEW_OWN')")
    public ResponseEntity<ConfigurationConge> getForEmployee(@PathVariable String employeeId) {
        log.info("🔍 GET /api/configurations/conges/employee/{}", employeeId);
        try {
            ConfigurationConge config = service.getConfigurationForEmployee(employeeId);
            log.info("✅ Configuration trouvée pour l'employé {}: type={}, jours={}",
                    employeeId, config.getType(), config.getJoursDeBase());
            return ResponseEntity.ok(config);
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration non trouvée pour l'employé: {}", employeeId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de la config pour l'employé {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employee/{employeeId}/individual")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH') or hasAuthority('LEAVE_VIEW_OWN')")
    public ResponseEntity<ConfigurationConge> getOrCreateIndividual(@PathVariable String employeeId) {
        log.info("🔍 GET /api/configurations/conges/employee/{}/individual", employeeId);
        try {
            ConfigurationConge config = service.getOrCreateIndividualConfiguration(employeeId);
            log.info("✅ Configuration individuelle récupérée/créée pour l'employé {}: jours={}",
                    employeeId, config.getJoursDeBase());
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération/création de la config individuelle pour {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/individual/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> updateIndividual(@PathVariable String id, @RequestBody ConfigurationConge config) {
        log.info("📥 PUT /api/configurations/conges/individual/{} - Corps reçu : {}", id, config);
        try {
            ConfigurationConge existing = service.getById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ConfigurationConge", id));

            if (!"INDIVIDUELLE".equals(existing.getType())) {
                return ResponseEntity.badRequest().body("Cette configuration n'est pas une configuration individuelle");
            }

            if (config.getEmployeeId() != null && !config.getEmployeeId().equals(existing.getEmployeeId())) {
                return ResponseEntity.badRequest().body("L'ID employé ne correspond pas à la configuration");
            }

            config.setEmployeeId(existing.getEmployeeId());
            config.setType("INDIVIDUELLE");

            ConfigurationConge updated = service.update(id, config);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration introuvable avec ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("⚠️ Erreur mise à jour : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ConfigurationConge config) {
        log.info("📥 PUT /api/configurations/conges/{} - Corps reçu : {}", id, config);
        try {
            ConfigurationConge updated = service.update(id, config);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration introuvable avec ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("⚠️ Erreur mise à jour : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        log.info("📥 DELETE /api/configurations/conges/{}", id);
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Configuration introuvable avec ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("⚠️ Erreur suppression : {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}