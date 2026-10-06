package com.fric.sirh.dashboard.service.impl;

import com.fric.sirh.dashboard.dto.DashboardEmployeeResponse;
import com.fric.sirh.dashboard.service.DashboardEmployeeService;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.model.Sanction;
import com.fric.sirh.discipline.repository.DemandeExplicationRepository;
import com.fric.sirh.discipline.repository.SanctionRepository;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Documents;
import com.fric.sirh.model.Employee;
import com.fric.sirh.performance.model.EvaluationPerformance;
import com.fric.sirh.performance.repository.EvaluationPerformanceRepository;
import com.fric.sirh.repository.CongeRepository;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.DocumentsRepository;
import com.fric.sirh.security.CurrentUserHelper;
import com.fric.sirh.service.CongeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardEmployeeServiceImpl implements DashboardEmployeeService {

    private final CurrentUserHelper currentUserHelper;
    private final CongeService congeService;
    private final CongeRepository congeRepository;
    private final ContratRepository contratRepository;
    private final DocumentsRepository documentsRepository;

    private final DemandeExplicationRepository demandeExplicationRepository;
    private final SanctionRepository sanctionRepository;
    private final EvaluationPerformanceRepository evaluationPerformanceRepository;

    @Override
    public DashboardEmployeeResponse getDashboard() {
        Employee employee = currentUserHelper.getCurrentEmployee();
        String employeeId = employee.getId();

        log.info("=== CHARGEMENT DASHBOARD EMPLOYÉ ===");
        log.info("Employé ID: {}", employeeId);

        // ========== CONGÉS ==========
        int leaveBalance = congeService.calculerSoldeRestant(employeeId);

        int currentYear = LocalDate.now().getYear();
        int takenLeaves = congeRepository.findByEmployeeAndTypeCongeAndStatutAndAnnee(
                employee,
                TypeConge.ANNUEL,
                StatutConge.APPROUVE,
                currentYear
        ).stream().mapToInt(Conge::getNbJour).sum();

        long pendingLeaves = congeRepository.countByEmployeeAndStatut(employee, StatutConge.EN_ATTENTE);

        // ========== CONTRAT ==========
        Contrat activeContract = contratRepository.findByEmployee_IdAndStatut(employeeId, "ACTIF")
                .stream().findFirst().orElse(null);
        DashboardEmployeeResponse.CurrentContract currentContract = activeContract != null ?
                DashboardEmployeeResponse.CurrentContract.builder()
                        .id(activeContract.getId())
                        .type(activeContract.getTypeContrat())
                        .startDate(activeContract.getDateDebut())
                        .endDate(activeContract.getDateFin())
                        .status(activeContract.getStatut())
                        .build() : null;

        // ========== DOCUMENTS ==========
        List<Documents> docs = documentsRepository.findByEmployee_Id(employeeId);
        List<DashboardEmployeeResponse.DocumentInfo> documents = docs.stream()
                .map(d -> DashboardEmployeeResponse.DocumentInfo.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .type(d.getTypeDocument())
                        .imageUrls(d.getImageUrls())
                        .uploadDate(d.getDateUpload())
                        .build())
                .collect(Collectors.toList());

        // ========== NOTIFICATIONS ==========
        List<DashboardEmployeeResponse.Notification> notifications = buildNotifications(employee, activeContract);

        // ========== DEMANDES D'EXPLICATION ==========
        log.info("Récupération des demandes d'explication pour l'employé {}", employeeId);
        List<DemandeExplication> allDemandes = demandeExplicationRepository.findByEmployeConcerneId(employeeId);
        log.info("Demandes d'explication trouvées: {}", allDemandes.size());

        long pendingExplanationRequests = allDemandes.stream()
                .filter(d -> d.getStatut() == com.fric.sirh.discipline.enums.StatutDemandeExplication.EN_ATTENTE)
                .count();

        List<DashboardEmployeeResponse.MonthlyEvolution> explanationEvolution =
                buildExplanationEvolution(allDemandes);

        // ========== SANCTIONS ==========
        log.info("Récupération des sanctions pour l'employé {}", employeeId);
        List<Sanction> allSanctions = sanctionRepository.findByEmploye_Id(employeeId);
        log.info("Sanctions trouvées: {}", allSanctions.size());

        long activeSanctions = allSanctions.stream()
                .filter(s -> s.getStatut() == com.fric.sirh.discipline.enums.StatutSanction.ACTIVE)
                .count();

        List<DashboardEmployeeResponse.SanctionTypeCount> sanctionsByType = allSanctions.stream()
                .filter(s -> s.getType() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getType().name(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> DashboardEmployeeResponse.SanctionTypeCount.builder()
                        .type(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // ========== PERFORMANCE ==========
        log.info("Récupération des évaluations pour l'employé {}", employeeId);
        List<EvaluationPerformance> allEvaluations = evaluationPerformanceRepository.findByEmployeId(employeeId);
        log.info("Évaluations trouvées: {}", allEvaluations.size());

        long pendingEvaluations = allEvaluations.stream()
                .filter(e -> e.getDateEvaluation() == null)
                .count();

        List<DashboardEmployeeResponse.PerformanceEvolution> performanceEvolution = buildPerformanceEvolution(allEvaluations);

        // ========== CONSTRUCTION DE LA RÉPONSE ==========
        DashboardEmployeeResponse response = DashboardEmployeeResponse.builder()
                .leaveBalance(leaveBalance)
                .takenLeaves(takenLeaves)
                .pendingLeaves(pendingLeaves)
                .currentContract(currentContract)
                .documents(documents)
                .notifications(notifications)
                .pendingExplanationRequests(pendingExplanationRequests)
                .activeSanctions(activeSanctions)
                .pendingEvaluations(pendingEvaluations)
                .explanationRequestsEvolution(explanationEvolution)
                .sanctionsByType(sanctionsByType)
                .performanceEvolution(performanceEvolution)
                .build();

        log.info("Dashboard employé construit avec: pendingDE={}, activeSanctions={}, pendingEval={}, sanctionsByType={}, perfEvolution={}",
                pendingExplanationRequests, activeSanctions, pendingEvaluations,
                sanctionsByType.size(), performanceEvolution.size());

        return response;
    }

    // ============================================================
    // MÉTHODES PRIVÉES
    // ============================================================

    private List<DashboardEmployeeResponse.Notification> buildNotifications(Employee employee, Contrat activeContract) {
        List<DashboardEmployeeResponse.Notification> notifications = new ArrayList<>();

        if (activeContract != null && activeContract.getDateFin() != null) {
            LocalDate today = LocalDate.now();
            long daysToExpiry = ChronoUnit.DAYS.between(today, activeContract.getDateFin());
            if (daysToExpiry <= 14 && daysToExpiry > 0) {
                String severity = daysToExpiry <= 7 ? "CRITICAL" : "WARNING";
                DashboardEmployeeResponse.ContractDetails details = DashboardEmployeeResponse.ContractDetails.builder()
                        .employeeName(employee.getPrenom() + " " + employee.getNom())
                        .contractType(activeContract.getTypeContrat())
                        .startDate(activeContract.getDateDebut().toString())
                        .endDate(activeContract.getDateFin().toString())
                        .status(activeContract.getStatut())
                        .daysRemaining(daysToExpiry)
                        .build();

                notifications.add(DashboardEmployeeResponse.Notification.builder()
                        .type("CONTRAT_EXPIRANT")
                        .severity(severity)
                        .message("Votre contrat expire dans " + daysToExpiry + " jours")
                        .contractDetails(details)
                        .build());
            }
        }

        List<String> existingTypes = documentsRepository.findByEmployee_Id(employee.getId())
                .stream()
                .map(Documents::getTypeDocument)
                .collect(Collectors.toList());
        List<String> mandatory = List.of("CNI", "CERTIFICAT", "PHOTO");
        List<String> missing = mandatory.stream()
                .filter(type -> !existingTypes.contains(type))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            notifications.add(DashboardEmployeeResponse.Notification.builder()
                    .type("DOCUMENT_MANQUANT")
                    .severity("CRITICAL")
                    .message("Documents manquants : " + String.join(", ", missing))
                    .details(missing)
                    .build());
        }

        long pending = congeRepository.countByEmployeeAndStatut(employee, StatutConge.EN_ATTENTE);
        if (pending > 0) {
            notifications.add(DashboardEmployeeResponse.Notification.builder()
                    .type("CONGES_EN_ATTENTE")
                    .severity("INFO")
                    .message("Vous avez " + pending + " demande(s) de congé en attente")
                    .details(pending)
                    .build());
        }

        return notifications;
    }

    private List<DashboardEmployeeResponse.MonthlyEvolution> buildExplanationEvolution(List<DemandeExplication> demandes) {
        Map<String, Long> monthMap = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(11).withDayOfMonth(1);

        for (DemandeExplication d : demandes) {
            LocalDateTime dateTime = d.getCreatedAt();
            if (dateTime == null) continue;
            LocalDate date = dateTime.toLocalDate();
            if (date.isBefore(startDate) || date.isAfter(today)) continue;

            String key = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + date.getYear();
            monthMap.put(key, monthMap.getOrDefault(key, 0L) + 1);
        }

        List<DashboardEmployeeResponse.MonthlyEvolution> evolution = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            String key = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + monthDate.getYear();
            long count = monthMap.getOrDefault(key, 0L);
            evolution.add(DashboardEmployeeResponse.MonthlyEvolution.builder()
                    .month(monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH))
                    .count(count)
                    .build());
        }
        return evolution;
    }

    private List<DashboardEmployeeResponse.PerformanceEvolution> buildPerformanceEvolution(List<EvaluationPerformance> evaluations) {
        Map<String, List<Double>> monthScores = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(11).withDayOfMonth(1);

        for (EvaluationPerformance eval : evaluations) {
            LocalDateTime dateTime = eval.getDateEvaluation();
            if (dateTime == null) continue;
            LocalDate date = dateTime.toLocalDate();
            if (date.isBefore(startDate) || date.isAfter(today)) continue;

            String key = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + date.getYear();
            monthScores.computeIfAbsent(key, k -> new ArrayList<>()).add(eval.getPourcentage());
        }

        List<DashboardEmployeeResponse.PerformanceEvolution> evolution = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            String key = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + monthDate.getYear();
            List<Double> scores = monthScores.get(key);
            double avg = scores != null && !scores.isEmpty() ? scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) : 0.0;
            evolution.add(DashboardEmployeeResponse.PerformanceEvolution.builder()
                    .month(monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH))
                    .averageScore(Math.round(avg * 10) / 10.0)
                    .build());
        }
        return evolution;
    }
}