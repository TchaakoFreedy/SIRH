// src/main/java/com/fric/sirh/cacao/controller/AvanceFinanciereController.java

package com.fric.sirh.cacao.controller;

import com.fric.sirh.cacao.dto.AvanceFinanciereDto;
import com.fric.sirh.cacao.service.AvanceFinanciereService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cacao/avances")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('RH', 'ADMIN', 'DIRECTION')")
public class AvanceFinanciereController {

    private final AvanceFinanciereService avanceService;

    @GetMapping
    public ResponseEntity<List<AvanceFinanciereDto>> getAll() {
        log.info("GET /api/cacao/avances - Recuperation de toutes les avances");
        return ResponseEntity.ok(avanceService.getAllAvances());
    }

    @GetMapping("/acheteur/{acheteurId}")
    public ResponseEntity<List<AvanceFinanciereDto>> getByAcheteur(@PathVariable String acheteurId) {
        log.info("GET /api/cacao/avances/acheteur/{} - Recuperation des avances pour l'acheteur", acheteurId);
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des avances avec un ID d'acheteur vide");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(avanceService.getAvancesByAcheteur(acheteurId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvanceFinanciereDto> getById(@PathVariable String id) {
        log.info("GET /api/cacao/avances/{} - Recuperation de l'avance", id);
        return ResponseEntity.ok(avanceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AvanceFinanciereDto> create(@Valid @RequestBody AvanceFinanciereDto dto) {
        log.info("POST /api/cacao/avances - Creation d'une avance pour l'acheteur {}", dto.getAcheteurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(avanceService.createAvance(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/cacao/avances/{} - Suppression de l'avance", id);
        avanceService.deleteAvance(id);
        return ResponseEntity.noContent().build();
    }
}