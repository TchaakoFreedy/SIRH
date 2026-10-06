package com.fric.sirh.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fric.sirh.dto.CongeRequest;
import com.fric.sirh.dto.AbsenceSignalRequest;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.ConfigurationConge;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.CongeRepository;
import com.fric.sirh.repository.EmployeeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CongeService {

    private final CongeRepository congeRepository;
    private final EmployeeRepository employeeRepository;
    private final ConfigurationCongeService configurationCongeService;
    private final NotificationPublisher notificationPublisher;

    public CongeService(CongeRepository congeRepository,
                        EmployeeRepository employeeRepository,
                        ConfigurationCongeService configurationCongeService,
                        NotificationPublisher notificationPublisher) {
        this.congeRepository = congeRepository;
        this.employeeRepository = employeeRepository;
        this.configurationCongeService = configurationCongeService;
        this.notificationPublisher = notificationPublisher;
    }

    // --- Méthodes existantes ---
    public List<Conge> getAll() {
        return congeRepository.findAll();
    }

    public Conge getById(String id) {
        return congeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Congé introuvable"));
    }

    private Employee findEmployeeByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new RuntimeException("L'identifiant employé est obligatoire");
        }
        log.debug("Recherche de l'employé avec identifiant: {}", identifier);

        Optional<Employee> employeeOpt = employeeRepository.findByMatriculeInterne(identifier);
        if (employeeOpt.isPresent()) {
            log.debug("Employé trouvé par matricule interne: {}", identifier);
            return employeeOpt.get();
        }

        employeeOpt = employeeRepository.findById(identifier);
        if (employeeOpt.isPresent()) {
            log.debug("Employé trouvé par ID: {}", identifier);
            return employeeOpt.get();
        }

        List<Employee> all = employeeRepository.findAll();
        String available = all.stream()
                .map(e -> "ID: " + e.getId() + ", Matricule: " + e.getMatriculeInterne())
                .collect(Collectors.joining(" | "));
        throw new RuntimeException("Employé introuvable avec l'identifiant: '" + identifier +
                "'. Employés disponibles: " + available);
    }

    @Transactional
    public Conge create(CongeRequest request) {
        if (request.getEmployeeId() == null) {
            throw new RuntimeException("L'identifiant employé est obligatoire");
        }
        Employee employee = findEmployeeByIdentifier(request.getEmployeeId());
        if (!TypeConge.ANNUEL.equals(request.getTypeConge())) {
            throw new RuntimeException("Seuls les congés annuels sont autorisés via cette méthode");
        }
        long jours = ChronoUnit.DAYS.between(request.getJourDebut(), request.getJourFin()) + 1;
        if (jours <= 0) {
            throw new RuntimeException("Les dates du congé sont invalides");
        }
        int solde = getSoldeDisponible(employee);
        if (jours > solde) {
            throw new RuntimeException("Solde insuffisant. Disponible : " + solde + " jours");
        }
        if (hasOverlappingLeave(employee, request.getJourDebut(), request.getJourFin())) {
            throw new RuntimeException("Vous avez déjà une demande de congé sur cette période");
        }
        Conge conge = Conge.builder()
                .employee(employee)
                .typeConge(request.getTypeConge())
                .jourDebut(request.getJourDebut())
                .jourFin(request.getJourFin())
                .nbJour((int) jours)
                .statut(StatutConge.EN_ATTENTE)
                .createdAt(LocalDate.now())
                .annee(LocalDate.now().getYear())
                .build();
        log.info("Demande de congé créée pour l'employé {} ({} jours)", employee.getId(), jours);
        Conge saved = congeRepository.save(conge);

        // 🔔 Publier événement LEAVE_REQUESTED
        publishLeaveEvent(saved, NotificationEvent.LEAVE_REQUESTED, null);

        return saved;
    }

    @Transactional
    public Conge signalAbsence(AbsenceSignalRequest request) {
        Employee employee = findEmployeeByIdentifier(request.getEmployeeId());
        long jours = ChronoUnit.DAYS.between(request.getJourDebut(), request.getJourFin()) + 1;
        if (jours <= 0) throw new RuntimeException("Dates d'absence invalides");
        Conge absence = Conge.builder()
                .employee(employee)
                .typeConge(TypeConge.ABSENCE)
                .jourDebut(request.getJourDebut())
                .jourFin(request.getJourFin())
                .nbJour((int) jours)
                .statut(StatutConge.APPROUVE)
                .commentaireManager(request.getMotif())
                .createdAt(LocalDate.now())
                .dateValidation(LocalDate.now())
                .annee(LocalDate.now().getYear())
                .build();
        log.info("Absence signalée pour l'employé {} ({} jours)", employee.getId(), jours);
        Conge saved = congeRepository.save(absence);

        // 🔔 Publier événement LEAVE_APPROVED pour absence
        publishLeaveEvent(saved, NotificationEvent.LEAVE_APPROVED, "SYSTEM");

        return saved;
    }

    @Transactional
    public Conge requestPermission(CongeRequest request) {
        Employee employee = findEmployeeByIdentifier(request.getEmployeeId());
        long jours = ChronoUnit.DAYS.between(request.getJourDebut(), request.getJourFin()) + 1;
        if (jours <= 0) throw new RuntimeException("Dates de permission invalides");
        if (jours > 3) throw new RuntimeException("La permission ne peut pas dépasser 3 jours");

        // ✅ La permission est créée en EN_ATTENTE et doit être validée par le manager
        Conge permission = Conge.builder()
                .employee(employee)
                .typeConge(TypeConge.PERMISSION)
                .jourDebut(request.getJourDebut())
                .jourFin(request.getJourFin())
                .nbJour((int) jours)
                .statut(StatutConge.EN_ATTENTE)
                .commentaireManager(request.getMotif())
                .createdAt(LocalDate.now())
                .annee(LocalDate.now().getYear())
                .build();

        log.info("Demande de permission créée pour l'employé {} ({} jours), en attente de validation", employee.getId(), jours);
        Conge saved = congeRepository.save(permission);

        // 🔔 Publier événement LEAVE_REQUESTED pour permission
        publishLeaveEvent(saved, NotificationEvent.LEAVE_REQUESTED, null);

        return saved;
    }

    public Conge update(String id, Conge conge) {
        Conge existant = getById(id);
        if (!StatutConge.EN_ATTENTE.equals(existant.getStatut())) {
            throw new RuntimeException("Impossible de modifier un congé déjà traité");
        }
        long jours = ChronoUnit.DAYS.between(conge.getJourDebut(), conge.getJourFin()) + 1;
        existant.setJourDebut(conge.getJourDebut());
        existant.setJourFin(conge.getJourFin());
        existant.setNbJour((int) jours);
        existant.setUpdatedAt(LocalDate.now());
        log.info("Congé {} mis à jour", id);
        return congeRepository.save(existant);
    }

    public Conge cancel(String id) {
        Conge conge = getById(id);
        if (!StatutConge.EN_ATTENTE.equals(conge.getStatut())) {
            throw new RuntimeException("Impossible d'annuler ce congé");
        }
        conge.setStatut(StatutConge.ANNULE);
        conge.setUpdatedAt(LocalDate.now());
        log.info("Congé {} annulé", id);
        Conge saved = congeRepository.save(conge);
        // 🔔 Publier événement LEAVE_CANCELLED
        publishLeaveEvent(saved, NotificationEvent.LEAVE_CANCELLED, null);
        return saved;
    }

    // ===================== MÉTHODES MODIFIÉES AVEC VÉRIFICATION DÉPARTEMENT =====================

    public Conge approve(String id, String managerId, String commentaire) {
        Conge conge = getById(id);
        if (!StatutConge.EN_ATTENTE.equals(conge.getStatut())) {
            throw new RuntimeException("Ce congé a déjà été traité");
        }

        // ✅ Vérifier que le manager peut approuver ce congé (même département)
        if (!canManagerViewLeave(managerId, id)) {
            throw new RuntimeException("Vous ne pouvez pas approuver les congés d'un autre département");
        }

        // ✅ Vérifier le solde pour les congés annuels UNIQUEMENT
        if (TypeConge.ANNUEL.equals(conge.getTypeConge())) {
            Employee employee = conge.getEmployee();
            int solde = getSoldeDisponible(employee);
            if (conge.getNbJour() > solde) {
                throw new RuntimeException("Solde insuffisant pour approuver ce congé. Disponible : " + solde + " jours");
            }
        }

        // ✅ Permission et Annuel peuvent être approuvés
        conge.setStatut(StatutConge.APPROUVE);
        conge.setManagerId(managerId);
        conge.setCommentaireManager(commentaire);
        conge.setDateValidation(LocalDate.now());
        conge.setUpdatedAt(LocalDate.now());
        log.info("Congé {} approuvé par {}", id, managerId);
        Conge saved = congeRepository.save(conge);

        // 🔔 Publier événement LEAVE_APPROVED
        publishLeaveEvent(saved, NotificationEvent.LEAVE_APPROVED, managerId);

        return saved;
    }

    public Conge reject(String id, String managerId, String commentaire) {
        Conge conge = getById(id);
        if (!StatutConge.EN_ATTENTE.equals(conge.getStatut())) {
            throw new RuntimeException("Ce congé a déjà été traité");
        }

        // ✅ Vérifier que le manager peut rejeter ce congé (même département)
        if (!canManagerViewLeave(managerId, id)) {
            throw new RuntimeException("Vous ne pouvez pas rejeter les congés d'un autre département");
        }

        conge.setStatut(StatutConge.REJETE);
        conge.setManagerId(managerId);
        conge.setCommentaireManager(commentaire);
        conge.setDateValidation(LocalDate.now());
        conge.setUpdatedAt(LocalDate.now());
        log.info("Congé {} rejeté par {}", id, managerId);
        Conge saved = congeRepository.save(conge);
        // 🔔 Publier événement LEAVE_REJECTED
        publishLeaveEvent(saved, NotificationEvent.LEAVE_REJECTED, managerId);
        return saved;
    }

    // ===================== MÉTHODES POUR LE FILTRAGE PAR DÉPARTEMENT =====================

    /**
     * ✅ Récupère les congés des employés du même département que le manager (version robuste)
     */
    public List<Conge> getCongesByDepartement(String departementId) {
        if (departementId == null || departementId.trim().isEmpty()) {
            log.warn("Tentative de récupération des congés avec un département null");
            return List.of();
        }

        log.info("📥 Récupération des congés pour le département: {}", departementId);

        // ✅ Récupérer tous les employés du département
        List<Employee> employees = employeeRepository.findByDepartementId(departementId);

        if (employees.isEmpty()) {
            log.info("Aucun employé trouvé pour le département: {}", departementId);
            return List.of();
        }

        // ✅ Extraire les IDs des employés
        List<String> employeeIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        log.info("{} employés trouvés pour le département {}, IDs: {}", employees.size(), departementId, employeeIds);

        // ✅ Essayer plusieurs méthodes de récupération
        List<Conge> conges = null;

        // 1. Essayer avec findByEmployeeIds (format DBRef)
        try {
            conges = congeRepository.findByEmployeeIds(employeeIds);
            log.info("🔍 Essai 1 - findByEmployeeIds: {} congés trouvés", conges != null ? conges.size() : 0);
        } catch (Exception e) {
            log.warn("⚠️ Erreur avec findByEmployeeIds: {}", e.getMessage());
        }

        // 2. Si aucun résultat, essayer avec findByEmployeeIdsSimple (format simple)
        if (conges == null || conges.isEmpty()) {
            try {
                conges = congeRepository.findByEmployeeIdsSimple(employeeIds);
                log.info("🔍 Essai 2 - findByEmployeeIdsSimple: {} congés trouvés", conges != null ? conges.size() : 0);
            } catch (Exception e) {
                log.warn("⚠️ Erreur avec findByEmployeeIdsSimple: {}", e.getMessage());
            }
        }

        // 3. Si aucun résultat, essayer avec findByEmployeeIdsAny (les deux formats)
        if (conges == null || conges.isEmpty()) {
            try {
                conges = congeRepository.findByEmployeeIdsAny(employeeIds);
                log.info("🔍 Essai 3 - findByEmployeeIdsAny: {} congés trouvés", conges != null ? conges.size() : 0);
            } catch (Exception e) {
                log.warn("⚠️ Erreur avec findByEmployeeIdsAny: {}", e.getMessage());
            }
        }

        // 4. Si aucun résultat, essayer avec findByEmployeeList (objets Employee complets)
        if (conges == null || conges.isEmpty()) {
            try {
                conges = congeRepository.findByEmployeeList(employees);
                log.info("🔍 Essai 4 - findByEmployeeList: {} congés trouvés", conges != null ? conges.size() : 0);
            } catch (Exception e) {
                log.warn("⚠️ Erreur avec findByEmployeeList: {}", e.getMessage());
            }
        }

        // 5. Si toujours aucun résultat, vérifier si des congés existent avec ces employés individuellement
        if (conges == null || conges.isEmpty()) {
            log.info("🔍 Aucun congé trouvé avec les méthodes groupées, vérification individuelle...");
            for (Employee emp : employees) {
                try {
                    List<Conge> empConges = congeRepository.findByEmployee(emp);
                    if (!empConges.isEmpty()) {
                        log.info("   ✅ Employé {} {} a {} congé(s)", emp.getPrenom(), emp.getNom(), empConges.size());
                        if (conges == null) {
                            conges = empConges;
                        } else {
                            conges.addAll(empConges);
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Erreur avec findByEmployee pour l'employé {}: {}", emp.getId(), e.getMessage());
                }
            }
        }

        // Log final
        if (conges == null) {
            conges = List.of();
        }
        log.info("✅ {} congés trouvés pour le département {}", conges.size(), departementId);

        // Log des détails des congés trouvés
        if (!conges.isEmpty()) {
            conges.forEach(c -> {
                log.info("  📄 Congé: Type={}, Statut={}, Debut={}, Fin={}, Employé={}",
                        c.getTypeConge(), c.getStatut(), c.getJourDebut(), c.getJourFin(),
                        c.getEmployee() != null ? c.getEmployee().getPrenom() + " " + c.getEmployee().getNom() : "null");
            });
        }

        return conges;
    }

    /**
     * ✅ Récupère les congés des employés du même département avec filtres (statut et type)
     */
    public List<Conge> getCongesByDepartement(String departementId, String statut, String type) {
        if (departementId == null || departementId.trim().isEmpty()) {
            log.warn("Tentative de récupération des congés avec un département null");
            return List.of();
        }

        // ✅ Récupérer tous les employés du département
        List<Employee> employees = employeeRepository.findByDepartementId(departementId);

        if (employees.isEmpty()) {
            return List.of();
        }

        // ✅ Extraire les IDs des employés
        List<String> employeeIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        List<Conge> conges = null;

        if (statut != null && !statut.isEmpty() && !statut.equals("TOUS")) {
            // Avec statut
            try {
                conges = congeRepository.findByEmployeeIdsAnyAndStatut(employeeIds, StatutConge.valueOf(statut));
                log.info("🔍 findByEmployeeIdsAnyAndStatut: {} congés trouvés", conges != null ? conges.size() : 0);
            } catch (Exception e) {
                log.warn("⚠️ Erreur avec findByEmployeeIdsAnyAndStatut: {}", e.getMessage());
            }

            if (conges == null || conges.isEmpty()) {
                try {
                    conges = congeRepository.findByEmployeeListAndStatut(employees, StatutConge.valueOf(statut));
                    log.info("🔍 findByEmployeeListAndStatut: {} congés trouvés", conges != null ? conges.size() : 0);
                } catch (Exception e) {
                    log.warn("⚠️ Erreur avec findByEmployeeListAndStatut: {}", e.getMessage());
                }
            }
        } else if (type != null && !type.isEmpty() && !type.equals("TOUS")) {
            // Avec type
            try {
                conges = congeRepository.findByEmployeeIdsAndTypeConge(employeeIds, TypeConge.valueOf(type));
                log.info("🔍 findByEmployeeIdsAndTypeConge: {} congés trouvés", conges != null ? conges.size() : 0);
            } catch (Exception e) {
                log.warn("⚠️ Erreur avec findByEmployeeIdsAndTypeConge: {}", e.getMessage());
            }

            if (conges == null || conges.isEmpty()) {
                try {
                    conges = congeRepository.findByEmployeeIdsSimpleAndTypeConge(employeeIds, TypeConge.valueOf(type));
                    log.info("🔍 findByEmployeeIdsSimpleAndTypeConge: {} congés trouvés", conges != null ? conges.size() : 0);
                } catch (Exception e) {
                    log.warn("⚠️ Erreur avec findByEmployeeIdsSimpleAndTypeConge: {}", e.getMessage());
                }
            }
        } else {
            // Sans filtre
            conges = getCongesByDepartement(departementId);
        }

        if (conges == null) {
            conges = List.of();
        }

        log.info("✅ {} congés trouvés pour le département {} (statut: {}, type: {})",
                conges.size(), departementId, statut, type);

        return conges;
    }

    /**
     * ✅ Vérifie si un manager peut voir un congé (même département)
     */
    public boolean canManagerViewLeave(String managerId, String congeId) {
        try {
            // ✅ Récupérer le manager par matricule ou ID
            Employee manager = employeeRepository.findByMatriculeInterne(managerId)
                    .orElse(null);

            if (manager == null) {
                manager = employeeRepository.findById(managerId)
                        .orElse(null);
            }

            if (manager == null) {
                log.warn("⚠️ Aucun employé trouvé pour le manager: {}", managerId);
                return false;
            }

            // ✅ Récupérer le congé
            Conge conge = getById(congeId);
            Employee employee = conge.getEmployee();

            // ✅ Comparer les départements
            boolean sameDepartment = manager.getDepartementId() != null &&
                    manager.getDepartementId().equals(employee.getDepartementId());

            log.debug("Manager {} ({}) peut-il voir le congé {} ? {} (Département manager: {}, Département employé: {})",
                    managerId, manager.getNomComplet(), congeId, sameDepartment,
                    manager.getDepartementId(), employee.getDepartementId());

            return sameDepartment;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des droits du manager: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Récupère les membres de l'équipe d'un manager (même département)
     */
    public List<Employee> getTeamMembers(String managerId) {
        Employee manager = employeeRepository.findByMatriculeInterne(managerId)
                .orElseThrow(() -> new RuntimeException("Manager non trouvé"));

        if (manager.getDepartementId() == null) {
            log.warn("Manager {} n'a pas de département assigné", managerId);
            return List.of();
        }

        return employeeRepository.findByDepartementId(manager.getDepartementId());
    }

    // ===================== MÉTHODES EXISTANTES =====================

    public List<Conge> getByEmployee(String employeeId) {
        Employee employee = findEmployeeByIdentifier(employeeId);
        return congeRepository.findByEmployee(employee);
    }

    public List<Conge> getByStatut(StatutConge statut) {
        return congeRepository.findByStatut(statut);
    }

    public List<Conge> getByType(TypeConge type) {
        return congeRepository.findByTypeConge(type);
    }

    public List<Conge> getByEmployeeAndYear(String employeeId, Integer annee) {
        Employee employee = findEmployeeByIdentifier(employeeId);
        return congeRepository.findByEmployeeAndAnnee(employee, annee);
    }

    public List<Conge> getPendingLeaves(String employeeId) {
        Employee employee = findEmployeeByIdentifier(employeeId);
        return congeRepository.findByEmployeeAndStatut(employee, StatutConge.EN_ATTENTE);
    }

    private boolean hasOverlappingLeave(Employee employee, LocalDate debut, LocalDate fin) {
        List<Conge> existing = congeRepository.findByEmployee(employee);
        return existing.stream()
                .filter(c -> StatutConge.EN_ATTENTE.equals(c.getStatut()) ||
                        StatutConge.APPROUVE.equals(c.getStatut()))
                .filter(c -> TypeConge.ANNUEL.equals(c.getTypeConge()) || TypeConge.PERMISSION.equals(c.getTypeConge()))
                .anyMatch(c -> !(c.getJourFin().isBefore(debut) || c.getJourDebut().isAfter(fin)));
    }

    // ===================== MÉTHODES DE CALCUL AVEC CONFIGURATION DYNAMIQUE =====================

    public int calculerDroitsAnnuels(Employee employee) {
        int currentYear = LocalDate.now().getYear();
        ConfigurationConge config = null;
        try {
            config = configurationCongeService.getConfigurationForEmployeeAndYear(employee.getId(), currentYear);
            log.debug("Configuration utilisée pour l'employé {} : {}", employee.getId(), config);
        } catch (Exception e) {
            log.warn("Impossible de récupérer la configuration pour l'employé {}, utilisation de la globale", employee.getId(), e);
            try {
                config = configurationCongeService.getGlobalConfiguration();
            } catch (Exception ex) {
                log.error("Aucune configuration disponible (ni spécifique, ni globale). Utilisation de valeurs par défaut.", ex);
                config = createDefaultConfiguration();
            }
        }

        if (config == null) {
            log.warn("Configuration null, utilisation des valeurs par défaut.");
            config = createDefaultConfiguration();
        }

        int droit = config.getJoursDeBase();
        log.debug("Droits de base pour {} : {}", employee.getId(), droit);

        if (Boolean.TRUE.equals(config.getBonusEnfantActif())) {
            String sexe = employee.getSexe();
            if (sexe != null && (sexe.equalsIgnoreCase("F") || sexe.equalsIgnoreCase("FEMME"))) {
                Integer enfants = employee.getNombreEnfantsMoinsDe7Ans();
                if (enfants != null && enfants > 0) {
                    int bonus = enfants * config.getJoursParEnfant();
                    droit += bonus;
                    log.debug("Bonus enfant pour {} : {} enfants * {} jours = {} jours supplémentaires",
                            employee.getId(), enfants, config.getJoursParEnfant(), bonus);
                }
            }
        }

        log.info("Droits annuels calculés pour {} : {} jours", employee.getId(), droit);
        return droit;
    }

    private ConfigurationConge createDefaultConfiguration() {
        ConfigurationConge defaultConfig = new ConfigurationConge();
        defaultConfig.setJoursDeBase(24);
        defaultConfig.setBonusEnfantActif(true);
        defaultConfig.setJoursParEnfant(2);
        defaultConfig.setAgeMaxEnfant(7);
        defaultConfig.setType("GLOBALE");
        defaultConfig.setNom("Configuration par défaut");
        return defaultConfig;
    }

    private int calculerJoursCongesPris(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return 0;
        }
        int currentYear = LocalDate.now().getYear();
        List<Conge> congesApprouves = congeRepository.findByEmployeeAndTypeCongeAndStatutAndAnnee(
                employee,
                TypeConge.ANNUEL,
                StatutConge.APPROUVE,
                currentYear
        );
        int pris = congesApprouves.stream().mapToInt(Conge::getNbJour).sum();
        log.debug("Jours déjà pris pour {} en {} : {}", employee.getId(), currentYear, pris);
        return pris;
    }

    public int getSoldeDisponible(Employee employee) {
        int droits = calculerDroitsAnnuels(employee);
        int pris = calculerJoursCongesPris(employee);
        int solde = Math.max(0, droits - pris);
        log.info("Solde disponible pour {} : {} jours (droits: {}, pris: {})",
                employee.getId(), solde, droits, pris);
        return solde;
    }

    public int calculerSoldeRestant(String employeeId) {
        Employee employee = findEmployeeByIdentifier(employeeId);
        return getSoldeDisponible(employee);
    }

    // ===================== MÉTHODE PRIVÉE POUR PUBLIER LES NOTIFICATIONS =====================

    private void publishLeaveEvent(Conge conge, NotificationEvent event, String triggeredBy) {
        try {
            Employee employee = conge.getEmployee();
            Map<String, Object> data = new HashMap<>();
            data.put("employeeId", employee.getId() != null ? employee.getId() : "");
            data.put("companyId", employee.getEntrepriseId() != null ? employee.getEntrepriseId() : "");
            data.put("departmentId", employee.getDepartementId() != null ? employee.getDepartementId() : "");
            data.put("entityId", conge.getId() != null ? conge.getId() : "");
            data.put("entityType", "CONGE");
            data.put("actionUrl", "/conges/" + (conge.getId() != null ? conge.getId() : ""));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("typeConge", conge.getTypeConge() != null ? conge.getTypeConge().name() : "");
            metadata.put("statut", conge.getStatut() != null ? conge.getStatut().name() : "");
            metadata.put("nbJour", conge.getNbJour() != null ? conge.getNbJour() : 0);
            data.put("metadata", metadata);

            if (triggeredBy == null) {
                triggeredBy = "SYSTEM";
            }
            notificationPublisher.publish(event, data, triggeredBy);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la publication de l'événement: {}", e.getMessage(), e);
        }
    }
}