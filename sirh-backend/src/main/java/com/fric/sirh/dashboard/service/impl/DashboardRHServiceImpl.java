package com.fric.sirh.dashboard.service.impl;

import com.fric.sirh.dashboard.dto.DashboardRHResponse;
import com.fric.sirh.dashboard.service.DashboardRHService;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.repository.*;
import com.fric.sirh.security.CurrentUserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardRHServiceImpl implements DashboardRHService {

    private final CurrentUserHelper currentUserHelper;
    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final ContratRepository contratRepository;
    private final CongeRepository congeRepository;
    private final DocumentsRepository documentsRepository;

    private static final List<String> MANDATORY_DOCUMENT_TYPES = List.of("CNI", "CERTIFICAT", "PHOTO");
    private static final int EXPIRING_DAYS = 14;

    @Override
    public DashboardRHResponse getDashboard() {
        long totalEmployees = employeeRepository.count();
        long totalDepartments = departementRepository.count();
        long totalPositions = posteRepository.count();
        long activeContracts = contratRepository.countByStatut("ACTIF");

        LocalDate today = LocalDate.now();

        // Employés en congé aujourd'hui
        List<Conge> onLeaveToday = congeRepository.findApprovedOnDateForAll(today);
        long employeesOnLeaveToday = onLeaveToday.stream()
                .filter(c -> c.getEmployee() != null)
                .map(c -> c.getEmployee().getId())
                .distinct()
                .count();

        // Demandes en attente
        List<Conge> pendingLeaves = congeRepository.findPendingAll();
        long pendingLeaveRequests = pendingLeaves.size();

        // Contrats expirant bientôt
        LocalDate start = today;
        LocalDate end = today.plusDays(EXPIRING_DAYS);
        List<Contrat> expiringContracts = contratRepository.findByStatutAndDateFinBetween("ACTIF", start, end);
        long contractsExpiringSoon = expiringContracts.size();

        // Documents manquants
        Long missingDocs = documentsRepository.countEmployeesMissingDocumentTypesAll(MANDATORY_DOCUMENT_TYPES);
        long missingDocuments = missingDocs != null ? missingDocs : 0L;

        // Répartition hommes/femmes
        List<EmployeeRepository.SexeCount> sexeCounts = employeeRepository.countBySexeGroupBySexeAll();
        DashboardRHResponse.GenderDistribution genderDist = buildGenderDistribution(sexeCounts);

        // Employés par département
        List<DashboardRHResponse.DepartmentEmployeeCount> deptCounts = buildDepartmentCountsAll();

        // Répartition des contrats
        List<ContratRepository.TypeCount> typeCounts = contratRepository.countActiveByTypeAll();
        List<DashboardRHResponse.ContractTypeCount> contractDist = typeCounts.stream()
                .map(tc -> DashboardRHResponse.ContractTypeCount.builder()
                        .type(tc.getId())
                        .count(tc.getCount())
                        .build())
                .collect(Collectors.toList());

        // Évolution des recrutements
        LocalDate recruitmentStart = today.minusMonths(11).withDayOfMonth(1);
        LocalDate recruitmentEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<EmployeeRepository.MonthlyCount> recruitmentCounts = employeeRepository.countByMonthOfDateEmbaucheAll(recruitmentStart, recruitmentEnd);
        Map<Integer, Long> recruitmentMap = recruitmentCounts.stream()
                .collect(Collectors.toMap(EmployeeRepository.MonthlyCount::getId, EmployeeRepository.MonthlyCount::getCount));
        List<DashboardRHResponse.MonthlyEvolution> recruitmentEvo = generateMonthlyEvolution(recruitmentMap, recruitmentStart);

        // Évolution des congés
        LocalDate leaveStart = today.minusMonths(11).withDayOfMonth(1);
        LocalDate leaveEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<CongeRepository.MonthlyCount> leaveCounts = congeRepository.countApprovedByMonthAll(leaveStart, leaveEnd);
        Map<Integer, Long> leaveMap = leaveCounts.stream()
                .collect(Collectors.toMap(CongeRepository.MonthlyCount::getId, CongeRepository.MonthlyCount::getCount));
        List<DashboardRHResponse.MonthlyEvolution> leaveEvo = generateMonthlyEvolution(leaveMap, leaveStart);

        // Activités récentes
        List<DashboardRHResponse.RecentActivity> recentActivities = buildRecentActivitiesAll();

        // Alertes enrichies
        List<DashboardRHResponse.Alert> alerts = buildAlerts(expiringContracts, pendingLeaves, missingDocuments);

        return DashboardRHResponse.builder()
                .totalEmployees(totalEmployees)
                .totalDepartments(totalDepartments)
                .totalPositions(totalPositions)
                .activeContracts(activeContracts)
                .employeesOnLeaveToday(employeesOnLeaveToday)
                .pendingLeaveRequests(pendingLeaveRequests)
                .contractsExpiringSoon(contractsExpiringSoon)
                .missingDocuments(missingDocuments)
                .genderDistribution(genderDist)
                .employeesByDepartment(deptCounts)
                .contractDistribution(contractDist)
                .recruitmentEvolution(recruitmentEvo)
                .leaveEvolution(leaveEvo)
                .recentActivities(recentActivities)
                .alerts(alerts)
                .build();
    }

    // ============================================================
    // MÉTHODES PRIVÉES
    // ============================================================

    private DashboardRHResponse.GenderDistribution buildGenderDistribution(List<EmployeeRepository.SexeCount> sexeCounts) {
        long male = 0, female = 0;
        for (EmployeeRepository.SexeCount sc : sexeCounts) {
            String sexe = sc.getId();
            if (sexe != null) {
                if (sexe.equalsIgnoreCase("M") || sexe.equalsIgnoreCase("MASCULIN")) {
                    male += sc.getCount();
                } else if (sexe.equalsIgnoreCase("F") || sexe.equalsIgnoreCase("FEMININ")) {
                    female += sc.getCount();
                }
            }
        }
        return DashboardRHResponse.GenderDistribution.builder().male(male).female(female).build();
    }

    private List<DashboardRHResponse.DepartmentEmployeeCount> buildDepartmentCountsAll() {
        List<Departement> allDepts = departementRepository.findAll();
        List<EmployeeRepository.DepartmentCount> counts = employeeRepository.countByDepartementIdGroupByDepartementAll();
        Map<String, String> deptNames = allDepts.stream()
                .collect(Collectors.toMap(Departement::getId, Departement::getName));
        return counts.stream()
                .map(dc -> {
                    String deptId = dc.getId();
                    String name = deptNames.getOrDefault(deptId, "Inconnu");
                    return DashboardRHResponse.DepartmentEmployeeCount.builder()
                            .departmentName(name)
                            .count(dc.getCount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<DashboardRHResponse.MonthlyEvolution> generateMonthlyEvolution(Map<Integer, Long> monthMap, LocalDate start) {
        List<DashboardRHResponse.MonthlyEvolution> evolutions = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = start.plusMonths(i);
            int monthNumber = monthDate.getMonthValue();
            long count = monthMap.getOrDefault(monthNumber, 0L);
            String monthLabel = monthDate.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.FRENCH);
            evolutions.add(DashboardRHResponse.MonthlyEvolution.builder()
                    .month(monthLabel)
                    .count(count)
                    .build());
        }
        return evolutions;
    }

    private List<DashboardRHResponse.RecentActivity> buildRecentActivitiesAll() {
        List<DashboardRHResponse.RecentActivity> activities = new ArrayList<>();

        congeRepository.findApprovedAll().stream()
                .filter(c -> c.getEmployee() != null)
                .sorted(Comparator.comparing(Conge::getDateValidation, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .forEach(c -> {
                    Employee emp = c.getEmployee();
                    String empName = emp.getPrenom() + " " + emp.getNom();
                    activities.add(DashboardRHResponse.RecentActivity.builder()
                            .date(c.getDateValidation() != null ? c.getDateValidation() : c.getUpdatedAt())
                            .type("CONGÉ")
                            .description("Congé " + c.getTypeConge() + " approuvé")
                            .employeeName(empName)
                            .build());
                });

        contratRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getEmployee() != null)
                .limit(5)
                .forEach(c -> {
                    Employee emp = c.getEmployee();
                    String empName = emp.getPrenom() + " " + emp.getNom();
                    activities.add(DashboardRHResponse.RecentActivity.builder()
                            .date(c.getCreatedAt() != null ? c.getCreatedAt() : c.getDateDebut())
                            .type("CONTRAT")
                            .description("Contrat " + c.getTypeContrat() + " créé")
                            .employeeName(empName)
                            .build());
                });

        employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(Employee::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .forEach(e -> {
                    activities.add(DashboardRHResponse.RecentActivity.builder()
                            .date(e.getCreatedAt() != null ? e.getCreatedAt() : e.getDate_embauche())
                            .type("EMBAUCHE")
                            .description("Nouvel employé " + e.getPrenom() + " " + e.getNom())
                            .employeeName(e.getPrenom() + " " + e.getNom())
                            .build());
                });

        return activities.stream()
                .sorted(Comparator.comparing(DashboardRHResponse.RecentActivity::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CONSTRUCTION DES ALERTES ENRICHIES
    // ============================================================

    private List<DashboardRHResponse.Alert> buildAlerts(List<Contrat> expiringContracts,
                                                        List<Conge> pendingLeaves,
                                                        long missingDocuments) {
        List<DashboardRHResponse.Alert> alerts = new ArrayList<>();

        // 1. Alertes détaillées pour chaque contrat expirant
        alerts.addAll(buildContractExpirationAlerts(expiringContracts));

        // 2. Alerte groupée pour les congés en attente (avec noms et types)
        if (!pendingLeaves.isEmpty()) {
            alerts.add(buildPendingLeaveAlert(pendingLeaves));
        }

        // 3. Alerte pour les documents manquants
        if (missingDocuments > 0) {
            alerts.add(buildMissingDocumentAlert(missingDocuments));
        }

        return alerts;
    }

    /**
     * Crée une alerte par contrat expirant, avec tous les détails.
     */
    private List<DashboardRHResponse.Alert> buildContractExpirationAlerts(List<Contrat> expiringContracts) {
        List<DashboardRHResponse.Alert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Contrat contrat : expiringContracts) {
            Employee emp = contrat.getEmployee();
            if (emp == null) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, contrat.getDateFin());
            String severity = daysRemaining <= 7 ? "CRITICAL" : "WARNING";

            DashboardRHResponse.ContractDetails details = DashboardRHResponse.ContractDetails.builder()
                    .employeeName(emp.getPrenom() + " " + emp.getNom())
                    .contractType(contrat.getTypeContrat())
                    .startDate(contrat.getDateDebut().toString())
                    .endDate(contrat.getDateFin().toString())
                    .status(contrat.getStatut())
                    .daysRemaining(daysRemaining)
                    .build();

            alerts.add(DashboardRHResponse.Alert.builder()
                    .type("CONTRAT_EXPIRANT")
                    .severity(severity)
                    .message("Contrat de " + emp.getPrenom() + " " + emp.getNom() + " expire dans " + daysRemaining + " jours")
                    .contractDetails(details)
                    .build());
        }

        return alerts;
    }

    /**
     * Alerte groupée pour les congés en attente, listant les employés et types.
     */
    private DashboardRHResponse.Alert buildPendingLeaveAlert(List<Conge> pendingLeaves) {
        // Construire une description avec les 5 premiers employés
        List<String> descriptions = pendingLeaves.stream()
                .filter(c -> c.getEmployee() != null)
                .limit(5)
                .map(c -> {
                    Employee emp = c.getEmployee();
                    return emp.getPrenom() + " " + emp.getNom() + " (" + c.getTypeConge() + ")";
                })
                .collect(Collectors.toList());

        StringBuilder message = new StringBuilder();
        message.append(pendingLeaves.size()).append(" demande(s) de congé en attente");
        if (!descriptions.isEmpty()) {
            message.append(" : ").append(String.join(", ", descriptions));
            if (pendingLeaves.size() > 5) {
                message.append(" et ").append(pendingLeaves.size() - 5).append(" autre(s)");
            }
        }

        return DashboardRHResponse.Alert.builder()
                .type("CONGES_EN_ATTENTE")
                .severity("INFO")
                .message(message.toString())
                .details((long) pendingLeaves.size())
                .build();
    }

    /**
     * Alerte pour les documents manquants.
     */
    private DashboardRHResponse.Alert buildMissingDocumentAlert(long missingCount) {
        return DashboardRHResponse.Alert.builder()
                .type("DOCUMENT_MANQUANT")
                .severity("CRITICAL")
                .message(missingCount + " employé(s) ont des documents obligatoires manquants (CNI, Certificat, Photo)")
                .details(missingCount)
                .build();
    }
}