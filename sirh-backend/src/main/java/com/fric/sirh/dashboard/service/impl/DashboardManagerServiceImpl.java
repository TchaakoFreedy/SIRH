package com.fric.sirh.dashboard.service.impl;

import com.fric.sirh.dashboard.dto.DashboardManagerResponse;
import com.fric.sirh.dashboard.service.DashboardManagerService;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Poste;
import com.fric.sirh.repository.CongeRepository;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.PosteRepository;
import com.fric.sirh.security.CurrentUserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardManagerServiceImpl implements DashboardManagerService {

    private final CurrentUserHelper currentUserHelper;
    private final EmployeeRepository employeeRepository;
    private final CongeRepository congeRepository;
    private final PosteRepository posteRepository;
    private final ContratRepository contratRepository;

    private static final int EXPIRING_DAYS = 14;

    @Override
    public DashboardManagerResponse getDashboard() {
        log.info("=== DEBUT CONSTRUCTION DASHBOARD MANAGER ===");

        try {
            Employee manager = currentUserHelper.getCurrentEmployee();
            String departementId = manager.getDepartementId();

            log.info("Manager: {} {} (ID: {})", manager.getPrenom(), manager.getNom(), manager.getId());
            log.info("Departement ID: {}", departementId);

            if (departementId == null) {
                log.error("Le manager n'a pas de departement associe");
                return buildEmptyResponse();
            }

            List<Employee> team = employeeRepository.findByDepartementId(departementId);
            List<String> employeeIds = team.stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());

            log.info("Employes du departement: {}", employeeIds.size());
            team.forEach(e -> log.info("   - {} {} (ID: {})", e.getPrenom(), e.getNom(), e.getId()));

            long teamSize = team.size();
            LocalDate today = LocalDate.now();
            log.info("Date du jour: {}", today);

            // Récupération des congés
            List<Conge> allConges = new ArrayList<>();
            try {
                allConges = congeRepository.findByEmployeeIds(employeeIds);
                log.info("Methode 1 - findByEmployeeIds: {} congés trouves", allConges.size());
            } catch (Exception e) {
                log.warn("Erreur avec findByEmployeeIds: {}", e.getMessage());
            }

            if (allConges.isEmpty()) {
                try {
                    allConges = congeRepository.findByEmployeeIdsSimple(employeeIds);
                    log.info("Methode 2 - findByEmployeeIdsSimple: {} congés trouves", allConges.size());
                } catch (Exception e) {
                    log.warn("Erreur avec findByEmployeeIdsSimple: {}", e.getMessage());
                }
            }

            if (allConges.isEmpty()) {
                try {
                    allConges = congeRepository.findByEmployeeIdsAny(employeeIds);
                    log.info("Methode 3 - findByEmployeeIdsAny: {} congés trouves", allConges.size());
                } catch (Exception e) {
                    log.warn("Erreur avec findByEmployeeIdsAny: {}", e.getMessage());
                }
            }

            if (allConges.isEmpty()) {
                log.warn("AUCUN congé trouvé pour les employés du département");
            } else {
                log.info("Total congés trouvés: {}", allConges.size());
                allConges.forEach(c -> log.info("   - Type: {}, Statut: {}, Debut: {}, Fin: {}, Employe: {}",
                        c.getTypeConge(), c.getStatut(), c.getJourDebut(), c.getJourFin(),
                        c.getEmployee() != null ? c.getEmployee().getPrenom() + " " + c.getEmployee().getNom() : "null"));
            }

            List<Conge> approvedConges = allConges.stream()
                    .filter(c -> StatutConge.APPROUVE.equals(c.getStatut()))
                    .collect(Collectors.toList());
            log.info("Congés APPROUVES: {}", approvedConges.size());

            List<Conge> pendingConges = allConges.stream()
                    .filter(c -> StatutConge.EN_ATTENTE.equals(c.getStatut()))
                    .collect(Collectors.toList());
            log.info("Congés EN_ATTENTE: {}", pendingConges.size());

            long absentToday = approvedConges.stream()
                    .filter(c -> c.getJourDebut() != null && c.getJourFin() != null)
                    .filter(c -> !c.getJourDebut().isAfter(today) && !c.getJourFin().isBefore(today))
                    .map(c -> c.getEmployee().getId())
                    .distinct()
                    .count();
            log.info("Absents aujourd'hui: {}", absentToday);

            long pendingApprovals = pendingConges.size();
            log.info("Congés en attente d'approbation: {}", pendingApprovals);

            double presenceRate = teamSize > 0 ? ((double) (teamSize - absentToday) / teamSize) * 100 : 0;
            log.info("Taux de présence: {:.1f}%", presenceRate);

            List<DashboardManagerResponse.PositionCount> positionsDist = buildPositionDistribution(team);
            log.info("Repartition des postes: {}", positionsDist);

            List<DashboardManagerResponse.MonthlyEvolution> leaveEvo = buildLeaveEvolution(approvedConges, today);
            log.info("Evolution des congés (12 mois):");
            leaveEvo.forEach(e -> log.info("   {}: {}", e.getMonth(), e.getCount()));

            List<DashboardManagerResponse.RecentActivity> recentActivities = buildRecentActivities(approvedConges);
            log.info("Activités recentes: {} activités", recentActivities.size());

            // Récupération des contrats expirants pour les alertes
            LocalDate start = today;
            LocalDate end = today.plusDays(EXPIRING_DAYS);
            List<Contrat> expiringContracts = new ArrayList<>();
            for (Employee emp : team) {
                List<Contrat> empContracts = contratRepository.findByEmployee_IdAndStatut(emp.getId(), "ACTIF");
                for (Contrat c : empContracts) {
                    if (c.getDateFin() != null && !c.getDateFin().isBefore(start) && !c.getDateFin().isAfter(end)) {
                        expiringContracts.add(c);
                    }
                }
            }
            log.info("Contrats expirant dans {} jours: {}", EXPIRING_DAYS, expiringContracts.size());

            // Construction des alertes enrichies
            List<DashboardManagerResponse.Alert> alerts = buildAlerts(expiringContracts, pendingConges);

            return DashboardManagerResponse.builder()
                    .teamSize(teamSize)
                    .employeesAbsentToday(absentToday)
                    .pendingApprovals(pendingApprovals)
                    .presenceRate(presenceRate)
                    .positionsDistribution(positionsDist)
                    .leaveEvolution(leaveEvo)
                    .recentActivities(recentActivities)
                    .alerts(alerts)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la construction du dashboard MANAGER", e);
            return buildEmptyResponse();
        }
    }

    // ============================================================
    // METHODES PRIVEES
    // ============================================================

    private DashboardManagerResponse buildEmptyResponse() {
        log.warn("Construction d'une réponse vide pour le dashboard MANAGER");
        return DashboardManagerResponse.builder()
                .teamSize(0)
                .employeesAbsentToday(0)
                .pendingApprovals(0)
                .presenceRate(0)
                .positionsDistribution(new ArrayList<>())
                .leaveEvolution(new ArrayList<>())
                .recentActivities(new ArrayList<>())
                .alerts(new ArrayList<>())
                .build();
    }

    private List<DashboardManagerResponse.PositionCount> buildPositionDistribution(List<Employee> team) {
        Map<String, Long> positionCounts = team.stream()
                .filter(e -> e.getPosteId() != null)
                .collect(Collectors.groupingBy(Employee::getPosteId, Collectors.counting()));

        List<DashboardManagerResponse.PositionCount> positionsDist = new ArrayList<>();
        for (Map.Entry<String, Long> entry : positionCounts.entrySet()) {
            String posteId = entry.getKey();
            String posteName = posteRepository.findById(posteId)
                    .map(Poste::getLibelle)
                    .orElse("Inconnu");
            positionsDist.add(DashboardManagerResponse.PositionCount.builder()
                    .positionName(posteName)
                    .count(entry.getValue())
                    .build());
        }
        return positionsDist;
    }

    private List<DashboardManagerResponse.MonthlyEvolution> buildLeaveEvolution(List<Conge> approvedConges, LocalDate today) {
        Map<Integer, Long> monthMap = new HashMap<>();
        LocalDate startDate = today.minusMonths(11).withDayOfMonth(1);

        for (Conge conge : approvedConges) {
            LocalDate date = conge.getDateValidation() != null ?
                    conge.getDateValidation() : conge.getCreatedAt();

            if (date != null) {
                int month = date.getMonthValue();
                int year = date.getYear();
                LocalDate monthDate = LocalDate.of(year, month, 1);
                if (!monthDate.isBefore(startDate) && !monthDate.isAfter(startDate.plusMonths(11))) {
                    monthMap.put(month, monthMap.getOrDefault(month, 0L) + 1);
                }
            }
        }

        log.info("Repartition des congés par mois: {}", monthMap);
        return generateMonthlyEvolution(monthMap, startDate);
    }

    private List<DashboardManagerResponse.MonthlyEvolution> generateMonthlyEvolution(Map<Integer, Long> monthMap, LocalDate startDate) {
        List<DashboardManagerResponse.MonthlyEvolution> evolutions = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            int monthNumber = monthDate.getMonthValue();
            long count = monthMap.getOrDefault(monthNumber, 0L);
            String monthLabel = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);

            evolutions.add(DashboardManagerResponse.MonthlyEvolution.builder()
                    .month(monthLabel)
                    .count(count)
                    .build());
        }

        return evolutions;
    }

    private List<DashboardManagerResponse.RecentActivity> buildRecentActivities(List<Conge> approvedConges) {
        return approvedConges.stream()
                .filter(c -> c.getEmployee() != null)
                .sorted(Comparator.comparing(
                        c -> c.getDateValidation() != null ? c.getDateValidation() : c.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(5)
                .map(c -> {
                    Employee emp = c.getEmployee();
                    String empName = emp.getPrenom() + " " + emp.getNom();
                    LocalDate date = c.getDateValidation() != null ?
                            c.getDateValidation() : c.getCreatedAt();
                    return DashboardManagerResponse.RecentActivity.builder()
                            .date(date)
                            .type("CONGE")
                            .description("Congé " + c.getTypeConge() + " approuvé (" + c.getNbJour() + " jours)")
                            .employeeName(empName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // CONSTRUCTION DES ALERTES ENRICHIES
    // ============================================================

    private List<DashboardManagerResponse.Alert> buildAlerts(List<Contrat> expiringContracts,
                                                             List<Conge> pendingConges) {
        List<DashboardManagerResponse.Alert> alerts = new ArrayList<>();

        // 1. Alertes détaillées pour chaque contrat expirant
        alerts.addAll(buildContractExpirationAlerts(expiringContracts));

        // 2. Alerte groupée pour les congés en attente (avec noms et types)
        if (!pendingConges.isEmpty()) {
            alerts.add(buildPendingLeaveAlert(pendingConges));
        }

        return alerts;
    }

    private List<DashboardManagerResponse.Alert> buildContractExpirationAlerts(List<Contrat> expiringContracts) {
        List<DashboardManagerResponse.Alert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Contrat contrat : expiringContracts) {
            Employee emp = contrat.getEmployee();
            if (emp == null) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, contrat.getDateFin());
            String severity = daysRemaining <= 7 ? "CRITICAL" : "WARNING";

            DashboardManagerResponse.ContractDetails details = DashboardManagerResponse.ContractDetails.builder()
                    .employeeName(emp.getPrenom() + " " + emp.getNom())
                    .contractType(contrat.getTypeContrat())
                    .startDate(contrat.getDateDebut().toString())
                    .endDate(contrat.getDateFin().toString())
                    .status(contrat.getStatut())
                    .daysRemaining(daysRemaining)
                    .build();

            alerts.add(DashboardManagerResponse.Alert.builder()
                    .type("CONTRAT_EXPIRANT")
                    .severity(severity)
                    .message("Contrat de " + emp.getPrenom() + " " + emp.getNom() + " expire dans " + daysRemaining + " jours")
                    .contractDetails(details)
                    .build());
        }

        return alerts;
    }

    private DashboardManagerResponse.Alert buildPendingLeaveAlert(List<Conge> pendingConges) {
        List<String> descriptions = pendingConges.stream()
                .filter(c -> c.getEmployee() != null)
                .limit(5)
                .map(c -> {
                    Employee emp = c.getEmployee();
                    return emp.getPrenom() + " " + emp.getNom() + " (" + c.getTypeConge() + ")";
                })
                .collect(Collectors.toList());

        StringBuilder message = new StringBuilder();
        message.append(pendingConges.size()).append(" demande(s) de congé en attente");
        if (!descriptions.isEmpty()) {
            message.append(" : ").append(String.join(", ", descriptions));
            if (pendingConges.size() > 5) {
                message.append(" et ").append(pendingConges.size() - 5).append(" autre(s)");
            }
        }

        return DashboardManagerResponse.Alert.builder()
                .type("CONGES_EN_ATTENTE")
                .severity("INFO")
                .message(message.toString())
                .details((long) pendingConges.size())
                .build();
    }
}