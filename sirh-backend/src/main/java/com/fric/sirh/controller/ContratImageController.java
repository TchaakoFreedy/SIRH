package com.fric.sirh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor
public class ContratImageController {

    @PostMapping("/{contratId}/images")
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<String> ajouterImages(@PathVariable String contratId) {
        return ResponseEntity.ok("Fonctionnalité d'upload d'images non implémentée");
    }

    @DeleteMapping("/{contratId}/images")
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<Void> supprimerImages(@PathVariable String contratId) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{contratId}/images/{imageIndex}")
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<Void> supprimerImage(@PathVariable String contratId, @PathVariable int imageIndex) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contratId}/images/replace")
    @PreAuthorize("hasAuthority('DOC_UPLOAD')")
    public ResponseEntity<String> remplacerImages(@PathVariable String contratId) {
        return ResponseEntity.ok("Fonctionnalité d'upload d'images non implémentée");
    }
}