// src/main/java/com/fric/sirh/cacao/controller/RemboursementController.java

package com.fric.sirh.cacao.controller;

import com.fric.sirh.cacao.dto.RemboursementDto;
import com.fric.sirh.cacao.service.RemboursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cacao/remboursements")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('RH', 'ADMIN', 'DIRECTION')")
public class RemboursementController {

    private final RemboursementService remboursementService;

    @GetMapping
    public ResponseEntity<List<RemboursementDto>> getAll() {
        log.info("GET /api/cacao/remboursements - Recuperation de tous les remboursements");
        return ResponseEntity.ok(remboursementService.getAllRemboursements());
    }

    @GetMapping("/acheteur/{acheteurId}")
    public ResponseEntity<List<RemboursementDto>> getByAcheteur(@PathVariable String acheteurId) {
        log.info("GET /api/cacao/remboursements/acheteur/{} - Recuperation des remboursements pour l'acheteur", acheteurId);
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des remboursements avec un ID d'acheteur vide");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(remboursementService.getRemboursementsByAcheteur(acheteurId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RemboursementDto> getById(@PathVariable String id) {
        log.info("GET /api/cacao/remboursements/{} - Recuperation du remboursement", id);
        return ResponseEntity.ok(remboursementService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RemboursementDto> create(@Valid @RequestBody RemboursementDto dto) {
        log.info("POST /api/cacao/remboursements - Creation d'un remboursement pour l'acheteur {}", dto.getAcheteurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(remboursementService.createRemboursement(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/cacao/remboursements/{} - Suppression du remboursement", id);
        remboursementService.deleteRemboursement(id);
        return ResponseEntity.noContent().build();
    }
}