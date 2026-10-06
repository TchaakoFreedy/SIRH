// src/main/java/com/fric/sirh/cacao/controller/AcheteurController.java

package com.fric.sirh.cacao.controller;

import com.fric.sirh.cacao.dto.AcheteurDto;
import com.fric.sirh.cacao.service.AcheteurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cacao/acheteurs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('RH', 'ADMIN', 'DIRECTION')")
public class AcheteurController {

    private final AcheteurService acheteurService;

    @GetMapping
    public ResponseEntity<List<AcheteurDto>> getAll() {
        log.info("GET /api/cacao/acheteurs - Recuperation de tous les acheteurs actifs");
        List<AcheteurDto> result = acheteurService.getAllAcheteursAvecSoldes();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AcheteurDto>> getAllIncluantSuspendus() {
        log.info("GET /api/cacao/acheteurs/all - Recuperation de tous les acheteurs (incluant suspendus)");
        List<AcheteurDto> result = acheteurService.getAllAcheteursAvecSoldesIncluantSuspendus();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcheteurDto> getById(@PathVariable String id) {
        log.info("GET /api/cacao/acheteurs/{} - Recuperation de l'acheteur", id);
        AcheteurDto result = acheteurService.getAcheteurAvecSolde(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<AcheteurDto> getByEmployeeId(@PathVariable String employeeId) {
        log.info("GET /api/cacao/acheteurs/employee/{} - Recuperation de l'acheteur par employe", employeeId);
        AcheteurDto result = acheteurService.findByEmployeeId(employeeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AcheteurDto> create(@Valid @RequestBody AcheteurDto dto) {
        log.info("POST /api/cacao/acheteurs - Creation ou reactivation d'un acheteur pour l'employe {}", dto.getEmployeeId());
        AcheteurDto result = acheteurService.create(dto);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcheteurDto> update(@PathVariable String id, @Valid @RequestBody AcheteurDto dto) {
        log.info("PUT /api/cacao/acheteurs/{} - Mise a jour de l'acheteur", id);
        AcheteurDto result = acheteurService.update(id, dto);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<AcheteurDto> reactivate(@PathVariable String id) {
        log.info("PATCH /api/cacao/acheteurs/{}/reactivate - Reactivation de l'acheteur", id);
        AcheteurDto result = acheteurService.reactivate(id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/cacao/acheteurs/{} - Desactivation de l'acheteur", id);
        acheteurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}