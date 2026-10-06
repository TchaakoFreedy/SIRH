package com.fric.sirh.controller;

import com.fric.sirh.dto.PaiementRequest;
import com.fric.sirh.dto.SoldeEmployeResponse;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Paiement;
import com.fric.sirh.security.CustomUserDetails;
import com.fric.sirh.service.PaiementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@CrossOrigin(origins = "http://localhost:4200")
public class PaiementController {

    private static final Logger log =
            LoggerFactory.getLogger(PaiementController.class);

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        return authentication.getName();
    }

    // ==========================================
    // SOLDES
    // ==========================================

    @GetMapping("/soldes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SoldeEmployeResponse>> getSoldes(
            @RequestParam("mois") Integer mois,
            @RequestParam("annee") Integer annee
    ) {
        return ResponseEntity.ok(
                paiementService.getSoldesAllEmployees(mois, annee)
        );
    }

    @GetMapping("/solde/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SoldeEmployeResponse> getSolde(
            @PathVariable("employeeId") String employeeId,
            @RequestParam("mois") Integer mois,
            @RequestParam("annee") Integer annee
    ) {
        return ResponseEntity.ok(
                paiementService.getSoldeEmploye(employeeId, mois, annee)
        );
    }

    // ==========================================
    // LISTES
    // ==========================================

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Paiement>> getAll() {
        return ResponseEntity.ok(paiementService.getAllPaiements());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Paiement>> getByEmployee(
            @PathVariable("employeeId") String employeeId
    ) {
        return ResponseEntity.ok(
                paiementService.getPaiementsByEmployee(employeeId)
        );
    }

    @GetMapping("/mois")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Paiement>> getByMois(
            @RequestParam("mois") Integer mois,
            @RequestParam("annee") Integer annee
    ) {
        return ResponseEntity.ok(
                paiementService.getPaiementsByMoisAnnee(mois, annee)
        );
    }

    // ==========================================
    // CREATION
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAuthority('PAYMENT_CREATE') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Paiement> create(
            @RequestBody PaiementRequest request,
            Authentication authentication
    ) {
        String creator = getCurrentUserId(authentication);
        Paiement created = paiementService.enregistrerPaiement(
                request,
                creator
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==========================================
    // RECU PDF
    // ==========================================

    @GetMapping("/{id}/recu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadRecu(@PathVariable("id") String id) {

        byte[] pdf = paiementService.generateRecuPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"recu_" + id + ".pdf\""
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    // ==========================================
    // SALAIRE MENSUEL
    // ==========================================

    @PatchMapping("/employee/{employeeId}/salaire")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Employee> setSalaire(
            @PathVariable("employeeId") String employeeId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        Object value = body.get("salaireMensuel");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        Double salaire;
        try {
            salaire = Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        String updater = getCurrentUserId(authentication);
        Employee updated = paiementService.setSalaireMensuel(
                employeeId,
                salaire,
                updater
        );
        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // SUPPRESSION
    // ==========================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_DELETE') or hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        paiementService.deletePaiement(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // HEALTH
    // ==========================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}