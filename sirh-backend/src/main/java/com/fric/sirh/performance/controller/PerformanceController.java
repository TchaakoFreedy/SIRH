package com.fric.sirh.performance.controller;

import com.fric.sirh.performance.dto.*;
import com.fric.sirh.performance.model.CriterePerformance;
import com.fric.sirh.performance.service.PerformanceService;
import com.fric.sirh.performance.mapper.CritereMapper;
import com.fric.sirh.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final CritereMapper critereMapper;

    // ==================== CRITÈRES ====================

    @GetMapping("/criteres")
    @PreAuthorize("hasAuthority('PERFORMANCE_CRITERIA_VIEW')")
    public ResponseEntity<List<CriterePerformanceDTO>> getAllCriteres() {
        return ResponseEntity.ok(performanceService.getAllCriteres());
    }

    @GetMapping("/criteres/active")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW_ALL')")
    public ResponseEntity<List<CriterePerformanceDTO>> getActiveCriteres() {
        return ResponseEntity.ok(performanceService.getActiveCriteres());
    }

    @GetMapping("/criteres/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<CriterePerformanceDTO> getCritereById(@PathVariable String id) {
        return ResponseEntity.ok(performanceService.getCritereById(id));
    }

    @PostMapping("/criteres")
    @PreAuthorize("hasAuthority('PERFORMANCE_CREATE')")
    public ResponseEntity<CriterePerformanceDTO> createCritere(@Valid @RequestBody CriterePerformanceDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Création de critère par l'utilisateur: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(performanceService.createCritere(dto, userId));
    }

    @PutMapping("/criteres/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_UPDATE')")
    public ResponseEntity<CriterePerformanceDTO> updateCritere(
            @PathVariable String id,
            @Valid @RequestBody CriterePerformanceDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.updateCritere(id, dto, userId));
    }

    @DeleteMapping("/criteres/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_DELETE')")
    public ResponseEntity<Void> deleteCritere(@PathVariable String id) {
        performanceService.deleteCritere(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ÉVALUATIONS ====================

    @GetMapping("/evaluations")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW_ALL')")
    public ResponseEntity<Page<EvaluationPerformanceDTO>> getAllEvaluations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(performanceService.getAllEvaluations(pageable));
    }

    @GetMapping("/evaluations/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<EvaluationPerformanceDTO> getEvaluationById(@PathVariable String id) {
        return ResponseEntity.ok(performanceService.getEvaluationById(id));
    }

    @GetMapping("/evaluations/employee/{employeeId}")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<Page<EvaluationPerformanceDTO>> getEvaluationsByEmployee(
            @PathVariable String employeeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(performanceService.getEvaluationsByEmployee(employeeId, pageable));
    }

    // Get current user's evaluations
    @GetMapping("/evaluations/my")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<Page<EvaluationPerformanceDTO>> getMyEvaluations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.getMyEvaluations(userId, pageable));
    }

    // Get current user's performance stats
    @GetMapping("/evaluations/my/stats")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<PerformanceStatsDTO> getMyPerformanceStats() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.getMyPerformanceStats(userId));
    }

    //  Get current user's performance evolution
    @GetMapping("/evaluations/my/evolution")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<EvolutionPerformanceDTO>> getMyPerformanceEvolution() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.getMyPerformanceEvolution(userId));
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAuthority('PERFORMANCE_CREATE')")
    public ResponseEntity<EvaluationPerformanceDTO> createEvaluation(@Valid @RequestBody EvaluationPerformanceDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Création d'évaluation par l'utilisateur: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(performanceService.createEvaluation(dto, userId));
    }

    @PutMapping("/evaluations/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_UPDATE')")
    public ResponseEntity<EvaluationPerformanceDTO> updateEvaluation(
            @PathVariable String id,
            @Valid @RequestBody EvaluationPerformanceDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.updateEvaluation(id, dto, userId));
    }

    @DeleteMapping("/evaluations/{id}")
    @PreAuthorize("hasAuthority('PERFORMANCE_DELETE')")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable String id) {
        performanceService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== CLASSEMENT ====================

    @GetMapping("/classement")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<ClassementDTO>> getClassement(
            @RequestParam Integer annee,
            @RequestParam(required = false) String entrepriseId,
            @RequestParam(required = false) String departementId,
            @RequestParam(required = false) Integer top) {
        return ResponseEntity.ok(performanceService.getClassement(annee, entrepriseId, departementId, top));
    }

    // Get current user's rank
    @GetMapping("/classement/my-rank")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<RankDTO> getMyRank(@RequestParam Integer annee) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(performanceService.getMyRank(userId, annee));
    }

    @GetMapping("/classement/top")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<ClassementDTO>> getTopEmployes(
            @RequestParam Integer annee,
            @RequestParam(defaultValue = "10") Integer top) {
        return ResponseEntity.ok(performanceService.getTopEmployes(annee, top));
    }

    @GetMapping("/classement/top/{top}")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<ClassementDTO>> getTopEmployesLimit(
            @PathVariable Integer top,
            @RequestParam Integer annee) {
        return ResponseEntity.ok(performanceService.getTopEmployes(annee, top));
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW_ALL')")
    public ResponseEntity<DashboardPerformanceDTO> getDashboard() {
        return ResponseEntity.ok(performanceService.getDashboard());
    }

    // ==================== NEW ENDPOINTS ====================

    @GetMapping("/criteres/employee/{employeeId}")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<CriterePerformanceDTO>> getCriteresForEmployee(@PathVariable String employeeId) {
        log.info("🔍 Récupération des critères pour l'employé: {}", employeeId);
        List<CriterePerformance> criteres = performanceService.getCriteresForEmployee(employeeId);
        List<CriterePerformanceDTO> dtos = criteres.stream()
                .map(critereMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/types-criteres")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<String>> getTypesCriteres() {
        List<String> types = Arrays.asList("GLOBAL", "SELECTIVE");
        return ResponseEntity.ok(types);
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('PERFORMANCE_VIEW')")
    public ResponseEntity<List<EmployeeSelectionDTO>> getEmployeesForSelection() {
        return ResponseEntity.ok(performanceService.getEmployeesForSelection());
    }
}