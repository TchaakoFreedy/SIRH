// src/main/java/com/fric/sirh/cacao/controller/ReceptionCacaoController.java

package com.fric.sirh.cacao.controller;

import com.fric.sirh.cacao.dto.ReceptionCacaoDto;
import com.fric.sirh.cacao.service.ReceptionCacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cacao/receptions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('RH', 'ADMIN', 'DIRECTION')")
public class ReceptionCacaoController {

    private final ReceptionCacaoService receptionService;

    @GetMapping
    public ResponseEntity<List<ReceptionCacaoDto>> getAll() {
        log.info("GET /api/cacao/receptions - Recuperation de toutes les receptions");
        return ResponseEntity.ok(receptionService.getAllReceptions());
    }

    @GetMapping("/acheteur/{acheteurId}")
    public ResponseEntity<List<ReceptionCacaoDto>> getByAcheteur(@PathVariable String acheteurId) {
        log.info("GET /api/cacao/receptions/acheteur/{} - Recuperation des receptions pour l'acheteur", acheteurId);
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des receptions avec un ID d'acheteur vide");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(receptionService.getReceptionsByAcheteur(acheteurId));
    }

    @GetMapping("/non-remboursees/acheteur/{acheteurId}")
    public ResponseEntity<List<ReceptionCacaoDto>> getNonRembourseesByAcheteur(@PathVariable String acheteurId) {
        log.info("GET /api/cacao/receptions/non-remboursees/acheteur/{} - Recuperation des receptions non remboursees", acheteurId);
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des receptions avec un ID d'acheteur vide");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(receptionService.getReceptionsNonRembourseesByAcheteur(acheteurId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceptionCacaoDto> getById(@PathVariable String id) {
        log.info("GET /api/cacao/receptions/{} - Recuperation de la reception", id);
        return ResponseEntity.ok(receptionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ReceptionCacaoDto> create(@Valid @RequestBody ReceptionCacaoDto dto) {
        log.info("POST /api/cacao/receptions - Creation d'une reception pour l'acheteur {}", dto.getAcheteurId());
        return ResponseEntity.status(HttpStatus.CREATED).body(receptionService.createReception(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/cacao/receptions/{} - Suppression de la reception", id);
        receptionService.deleteReception(id);
        return ResponseEntity.noContent().build();
    }
}