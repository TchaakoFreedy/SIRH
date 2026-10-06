package com.fric.sirh.dashboard.service.impl;

import com.fric.sirh.dashboard.dto.DashboardDirectionResponse;
import com.fric.sirh.dashboard.service.DashboardDirectionService;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.model.*;
import com.fric.sirh.repository.*;
import com.fric.sirh.security.CurrentUserHelper;
import com.fric.sirh.service.EntrepriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardDirectionServiceImpl implements DashboardDirectionService {

    private final CurrentUserHelper currentUserHelper;
    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;
    private final ContratRepository contratRepository;
    private final CongeRepository congeRepository;
    private final EntrepriseService entrepriseService;

    private static final int EXPIRING_DAYS = 14;

    @Override
    public DashboardDirectionResponse getDashboard() {
        log.info("=== DEBUT CONSTRUCTION DASHBOARD DIRECTION ===");

        try {
            Employee currentEmployee = currentUserHelper.getCurrentEmployee();
            log.info("Employe connecte: {} {} (ID: {})", currentEmployee.getPrenom(), currentEmployee.getNom(), currentEmployee.getId());

            Entreprise entreprise = entrepriseService.getEntrepriseByEmployeeId(currentEmployee.getId());
            if (entreprise == null) {
                log.error("Aucune entreprise trouvee pour l'utilisateur");
                return buildEmptyResponse();
            }
            log.info("Entreprise: {} (ID: {})", entreprise.getName(), entreprise.getId());

            List<Departement> departements = departementRepository.findByEntrepriseId(entreprise.getId());
            long totalDepartments = departements.size();
            log.info("Departements trouves: {}", totalDepartments);

            List<Employee> employees = employeeRepository.findByEntrepriseId(entreprise.getId());
            List<String> employeeIds = employees.stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());
            log.info("Employes trouves: {}", employeeIds.size());

            if (employees.isEmpty()) {
                log.warn("Aucun employe trouve pour l'entreprise {}", entreprise.getName());
                return buildEmptyResponse();
            }

            long totalEmployees = employeeIds.size();

            List<ObjectId> employeeObjectIds = employeeIds.stream()
                    .map(ObjectId::new)
                    .collect(Collectors.toList());

            long totalContracts = contratRepository.countByEmployeeIdInAndStatut(employeeObjectIds, "ACTIF");
            log.info("Statistiques: Employes={}, Departements={}, Contrats actifs={}", totalEmployees, totalDepartments, totalContracts);

            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusMonths(11).withDayOfMonth(1);

            List<DashboardDirectionResponse.MonthlyEvolution> employeeEvo = buildEmployeeEvolution(employees, startDate);
            log.info("Evolution employes: {} mois", employeeEvo.size());

            List<DashboardDirectionResponse.MonthlyEvolution> recruitmentEvo = buildRecruitmentEvolution(employees, startDate);
            log.info("Evolution recrutements: {} mois", recruitmentEvo.size());

            List<DashboardDirectionResponse.MonthlyEvolution> leaveEvo = buildLeaveEvolution(employeeObjectIds, startDate);
            log.info("Evolution congés: {} mois", leaveEvo.size());
            leaveEvo.forEach(e -> log.info("   {}: {}", e.getMonth(), e.getCount()));

            DashboardDirectionResponse.GenderDistribution genderDist = buildGenderDistribution(employees);

            List<Contrat> allActiveContracts = contratRepository.findByEmployeeIdIn(employeeObjectIds)
                    .stream()
                    .filter(c -> "ACTIF".equals(c.getStatut()))
                    .collect(Collectors.toList());

            List<DashboardDirectionResponse.ContractTypeCount> contractDist = buildContractDistribution(allActiveContracts);

            if (contractDist.isEmpty()) {
                log.info("Aucun contrat actif trouve, ajout d'un element factice pour le graphique");
                contractDist.add(DashboardDirectionResponse.ContractTypeCount.builder()
                        .type("Aucun contrat actif")
                        .count(0L)
                        .build());
            }
            log.info("Repartition contrats: {}", contractDist);

            // Récupération des contrats expirant dans les 14 jours
            LocalDate start = today;
            LocalDate end = today.plusDays(EXPIRING_DAYS);
            List<Contrat> expiringContracts = allActiveContracts.stream()
                    .filter(c -> c.getDateFin() != null)
                    .filter(c -> !c.getDateFin().isBefore(start) && !c.getDateFin().isAfter(end))
                    .collect(Collectors.toList());
            log.info("Contrats expirant dans {} jours: {}", EXPIRING_DAYS, expiringContracts.size());

            // Congés en attente
            List<Conge> pendingConges = congeRepository.findByEmployeeObjectIdsAndStatut(employeeObjectIds, StatutConge.EN_ATTENTE);
            long pendingCount = pendingConges != null ? pendingConges.size() : 0;
            log.info("Congés en attente: {}", pendingCount);

            // Construction des alertes enrichies
            List<DashboardDirectionResponse.Alert> alerts = buildAlerts(expiringContracts, pendingConges);

            return DashboardDirectionResponse.builder()
                    .totalEmployees(totalEmployees)
                    .totalDepartments(totalDepartments)
                    .totalContracts(totalContracts)
                    .employeeEvolution(employeeEvo)
                    .recruitmentEvolution(recruitmentEvo)
                    .leaveEvolution(leaveEvo)
                    .genderDistribution(genderDist)
                    .contractDistribution(contractDist)
                    .alerts(alerts)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la construction du dashboard DIRECTION", e);
            return buildEmptyResponse();
        }
    }

    // ============================================================
    // METHODES PRIVEES
    // ============================================================

    private DashboardDirectionResponse buildEmptyResponse() {
        log.warn("Construction d'une réponse vide");
        return DashboardDirectionResponse.builder()
                .totalEmployees(0)
                .totalDepartments(0)
                .totalContracts(0)
                .employeeEvolution(new ArrayList<>())
                .recruitmentEvolution(new ArrayList<>())
                .leaveEvolution(new ArrayList<>())
                .genderDistribution(DashboardDirectionResponse.GenderDistribution.builder().male(0).female(0).build())
                .contractDistribution(new ArrayList<>())
                .alerts(new ArrayList<>())
                .build();
    }

    private DashboardDirectionResponse.GenderDistribution buildGenderDistribution(List<Employee> employees) {
        long male = employees.stream()
                .filter(e -> e.getSexe() != null && ("M".equalsIgnoreCase(e.getSexe()) || "MASCULIN".equalsIgnoreCase(e.getSexe())))
                .count();
        long female = employees.stream()
                .filter(e -> e.getSexe() != null && ("F".equalsIgnoreCase(e.getSexe()) || "FEMININ".equalsIgnoreCase(e.getSexe())))
                .count();
        return DashboardDirectionResponse.GenderDistribution.builder().male(male).female(female).build();
    }

    private List<DashboardDirectionResponse.ContractTypeCount> buildContractDistribution(List<Contrat> contrats) {
        Map<String, Long> countByType = contrats.stream()
                .filter(c -> c.getTypeContrat() != null)
                .collect(Collectors.groupingBy(Contrat::getTypeContrat, Collectors.counting()));
        return countByType.entrySet().stream()
                .map(e -> DashboardDirectionResponse.ContractTypeCount.builder()
                        .type(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardDirectionResponse.MonthlyEvolution> buildEmployeeEvolution(List<Employee> employees, LocalDate startDate) {
        Map<Integer, Long> monthMap = new HashMap<>();
        long cumulative = 0;

        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            int month = monthDate.getMonthValue();
            int year = monthDate.getYear();

            long added = employees.stream()
                    .filter(e -> e.getDate_embauche() != null)
                    .filter(e -> e.getDate_embauche().getMonthValue() == month && e.getDate_embauche().getYear() == year)
                    .count();
            cumulative += added;
            monthMap.put(month, cumulative);
        }
        return generateMonthlyEvolution(monthMap, startDate);
    }

    private List<DashboardDirectionResponse.MonthlyEvolution> buildRecruitmentEvolution(List<Employee> employees, LocalDate startDate) {
        Map<Integer, Long> monthMap = new HashMap<>();

        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            int month = monthDate.getMonthValue();
            int year = monthDate.getYear();

            long count = employees.stream()
                    .filter(e -> e.getDate_embauche() != null)
                    .filter(e -> e.getDate_embauche().getMonthValue() == month && e.getDate_embauche().getYear() == year)
                    .count();
            monthMap.put(month, count);
        }
        return generateMonthlyEvolution(monthMap, startDate);
    }

    private List<DashboardDirectionResponse.MonthlyEvolution> buildLeaveEvolution(List<ObjectId> employeeObjectIds, LocalDate startDate) {
        Map<Integer, Long> monthMap = new HashMap<>();

        try {
            List<Conge> approvedLeaves = congeRepository.findByEmployeeObjectIdsAndStatut(employeeObjectIds, StatutConge.APPROUVE);

            log.info("Conges approuves trouves: {}", approvedLeaves != null ? approvedLeaves.size() : 0);

            if (approvedLeaves != null && !approvedLeaves.isEmpty()) {
                List<Conge> validLeaves = approvedLeaves.stream()
                        .filter(c -> c.getDateValidation() != null)
                        .collect(Collectors.toList());

                log.info("Conges avec date de validation: {}", validLeaves.size());

                if (validLeaves.isEmpty()) {
                    log.warn("Aucun congé avec dateValidation, utilisation de la date de creation");
                    validLeaves = approvedLeaves.stream()
                            .filter(c -> c.getCreatedAt() != null)
                            .collect(Collectors.toList());
                    log.info("Conges avec date de creation: {}", validLeaves.size());
                }

                for (Conge conge : validLeaves) {
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
            } else {
                log.info("Aucun congé approuvé trouvé pour ces employés");
            }

        } catch (Exception e) {
            log.error("Erreur lors du calcul de l'evolution des congés", e);
        }

        return generateMonthlyEvolution(monthMap, startDate);
    }

    private List<DashboardDirectionResponse.MonthlyEvolution> generateMonthlyEvolution(Map<Integer, Long> monthMap, LocalDate startDate) {
        List<DashboardDirectionResponse.MonthlyEvolution> evolutions = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            int monthNumber = monthDate.getMonthValue();
            long count = monthMap.getOrDefault(monthNumber, 0L);
            String monthLabel = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);

            evolutions.add(DashboardDirectionResponse.MonthlyEvolution.builder()
                    .month(monthLabel)
                    .count(count)
                    .build());
        }

        return evolutions;
    }

    // ============================================================
    // CONSTRUCTION DES ALERTES ENRICHIES
    // ============================================================

    private List<DashboardDirectionResponse.Alert> buildAlerts(List<Contrat> expiringContracts,
                                                               List<Conge> pendingConges) {
        List<DashboardDirectionResponse.Alert> alerts = new ArrayList<>();

        // 1. Alertes détaillées pour chaque contrat expirant
        alerts.addAll(buildContractExpirationAlerts(expiringContracts));

        // 2. Alerte groupée pour les congés en attente (avec noms et types)
        if (pendingConges != null && !pendingConges.isEmpty()) {
            alerts.add(buildPendingLeaveAlert(pendingConges));
        }

        return alerts;
    }

    private List<DashboardDirectionResponse.Alert> buildContractExpirationAlerts(List<Contrat> expiringContracts) {
        List<DashboardDirectionResponse.Alert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Contrat contrat : expiringContracts) {
            Employee emp = contrat.getEmployee();
            if (emp == null) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(today, contrat.getDateFin());
            String severity = daysRemaining <= 7 ? "CRITICAL" : "WARNING";

            DashboardDirectionResponse.ContractDetails details = DashboardDirectionResponse.ContractDetails.builder()
                    .employeeName(emp.getPrenom() + " " + emp.getNom())
                    .contractType(contrat.getTypeContrat())
                    .startDate(contrat.getDateDebut().toString())
                    .endDate(contrat.getDateFin().toString())
                    .status(contrat.getStatut())
                    .daysRemaining(daysRemaining)
                    .build();

            alerts.add(DashboardDirectionResponse.Alert.builder()
                    .type("CONTRAT_EXPIRANT")
                    .severity(severity)
                    .message("Contrat de " + emp.getPrenom() + " " + emp.getNom() + " expire dans " + daysRemaining + " jours")
                    .contractDetails(details)
                    .build());
        }

        return alerts;
    }

    private DashboardDirectionResponse.Alert buildPendingLeaveAlert(List<Conge> pendingConges) {
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

        return DashboardDirectionResponse.Alert.builder()
                .type("CONGES_EN_ATTENTE")
                .severity("INFO")
                .message(message.toString())
                .details(pendingConges.size())
                .build();
    }
}