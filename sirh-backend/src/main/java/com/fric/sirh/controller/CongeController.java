package com.fric.sirh.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fric.sirh.dto.CongeRequest;
import com.fric.sirh.dto.CongeValidationRequest;
import com.fric.sirh.dto.AbsenceSignalRequest;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.service.CongeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/conges")
@CrossOrigin("*")
public class CongeController {

    private final CongeService service;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public CongeController(CongeService service,
                           EmployeeRepository employeeRepository,
                           UserRepository userRepository) {
        this.service = service;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    // Méthode utilitaire pour trouver un employé par différents identifiants
    private Employee findEmployeeByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        log.debug("Recherche de l'employe pour userId: {}", userId);

        // 1. Essayer par matriculeInterne
        Optional<Employee> employeeOpt = employeeRepository.findByMatriculeInterne(userId);
        if (employeeOpt.isPresent()) {
            log.debug("Employe trouve par matriculeInterne: {}", userId);
            return employeeOpt.get();
        }

        // 2. Essayer par ID
        try {
            employeeOpt = employeeRepository.findById(userId);
            if (employeeOpt.isPresent()) {
                log.debug("Employe trouve par ID: {}", userId);
                return employeeOpt.get();
            }
        } catch (Exception e) {
            log.debug("L'ID {} n'est pas un ObjectId valide", userId);
        }

        // 3. Essayer par email via la table User (méthode existante findByUserEmail)
        if (userId.contains("@")) {
            try {
                employeeOpt = employeeRepository.findByUserEmail(userId);
                if (employeeOpt.isPresent()) {
                    log.debug("Employe trouve par User.email: {}", userId);
                    return employeeOpt.get();
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la recherche par User.email: {}", e.getMessage());
            }

            // Alternative: chercher l'utilisateur par email puis l'employé
            try {
                Optional<User> userOpt = userRepository.findByEmail(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    String employeeId = user.getEmployeeId();
                    if (employeeId != null && !employeeId.isEmpty()) {
                        employeeOpt = employeeRepository.findById(employeeId);
                        if (employeeOpt.isPresent()) {
                            log.debug("Employe trouve via User.email: {} -> Employee ID: {}", userId, employeeId);
                            return employeeOpt.get();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Erreur lors de la recherche par email via User: {}", e.getMessage());
            }
        }

        // 4. Récupérer l'employeeId depuis l'utilisateur par ID
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String employeeId = user.getEmployeeId();
                if (employeeId != null && !employeeId.isEmpty()) {
                    log.debug("Recherche d'employe via employeeId de l'utilisateur: {}", employeeId);
                    employeeOpt = employeeRepository.findById(employeeId);
                    if (employeeOpt.isPresent()) {
                        log.debug("Employe trouve via employeeId: {}", employeeId);
                        return employeeOpt.get();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche via employeeId: {}", e.getMessage());
        }

        // 5. Recherche exhaustive dans tous les employés (dernier recours)
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            for (Employee emp : allEmployees) {
                if (userId.equals(emp.getMatriculeInterne()) ||
                        userId.equals(emp.getId())) {
                    log.debug("Employe trouve par recherche exhaustive: {} {}", emp.getPrenom(), emp.getNom());
                    return emp;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche exhaustive: {}", e.getMessage());
        }

        log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
        return null;
    }

    // Méthode utilitaire pour vérifier si l'utilisateur est admin (RH, SUPER_ADMIN, DIRECTION)
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("RH") ||
                        a.getAuthority().equals("SUPER_ADMIN") ||
                        a.getAuthority().equals("ROLE_RH") ||
                        a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                        a.getAuthority().equals("DIRECTION") ||
                        a.getAuthority().equals("ROLE_DIRECTION"));
    }

    // Méthode utilitaire pour vérifier si l'utilisateur est TOP_MANAGER
    private boolean isTopManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("TOP_MANAGER") ||
                        a.getAuthority().equals("ROLE_TOP_MANAGER"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL')")
    public List<Conge> getAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        log.info("Recuperation des conges pour l'utilisateur: {}", userId);

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Acces a tous les conges");
            return service.getAll();
        }

        Employee manager = findEmployeeByUserId(userId);
        if (manager == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}, retourne liste vide", userId);
            return List.of();
        }

        if (manager.getDepartementId() == null || manager.getDepartementId().isEmpty()) {
            log.warn("L'employe {} ({} {}) n'a pas de departement assigne, retourne liste vide",
                    userId, manager.getPrenom(), manager.getNom());
            return List.of();
        }

        log.info("Manager {} ({}) - Acces aux conges des employes du departement {}",
                userId, manager.getNomComplet(), manager.getDepartementId());

        List<Conge> conges = service.getCongesByDepartement(manager.getDepartementId());
        log.info("{} conges trouves pour le departement {}", conges.size(), manager.getDepartementId());

        return conges;
    }

    @GetMapping("/departement")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_ALL')")
    public List<Conge> getCongesByDepartement(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String type) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Recuperation de tous les conges avec filtres");
            if (statut != null && !statut.isEmpty() && !statut.equals("TOUS")) {
                return service.getByStatut(StatutConge.valueOf(statut));
            }
            if (type != null && !type.isEmpty() && !type.equals("TOUS")) {
                return service.getByType(TypeConge.valueOf(type));
            }
            return service.getAll();
        }

        Employee manager = findEmployeeByUserId(userId);
        if (manager == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return List.of();
        }

        if (manager.getDepartementId() == null || manager.getDepartementId().isEmpty()) {
            log.warn("L'employe {} n'a pas de departement assigne", userId);
            return List.of();
        }

        log.info("Manager {} - Recuperation des conges du departement {} (statut: {}, type: {})",
                userId, manager.getDepartementId(), statut, type);

        return service.getCongesByDepartement(manager.getDepartementId(), statut, type);
    }

    @GetMapping("/team/members")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_ALL')")
    public ResponseEntity<List<Employee>> getTeamMembers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Recuperation de tous les employes");
            return ResponseEntity.ok(employeeRepository.findAll());
        }

        Employee manager = findEmployeeByUserId(userId);
        if (manager == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return ResponseEntity.ok(List.of());
        }

        if (manager.getDepartementId() == null || manager.getDepartementId().isEmpty()) {
            log.warn("L'employe {} n'a pas de departement assigne", userId);
            return ResponseEntity.ok(List.of());
        }

        List<Employee> team = employeeRepository.findByDepartementId(manager.getDepartementId());
        log.info("Manager {} - Equipe de {} membres", userId, team.size());

        return ResponseEntity.ok(team);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL') or hasAuthority('LEAVE_VIEW_TEAM')")
    public Conge getById(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Acces au conge {}", id);
            return service.getById(id);
        }

        if (!service.canManagerViewLeave(userId, id)) {
            throw new RuntimeException("Vous ne pouvez pas voir les conges d'un autre departement");
        }

        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public Conge create(@RequestBody CongeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de creer des conges (lecture seule)");
        }
        return service.create(request);
    }

    @PostMapping("/absence")
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public Conge signalAbsence(@RequestBody AbsenceSignalRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de signaler une absence (lecture seule)");
        }
        return service.signalAbsence(request);
    }

    @PostMapping("/permission")
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public Conge requestPermission(@RequestBody CongeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de demander une permission (lecture seule)");
        }
        return service.requestPermission(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public Conge update(@PathVariable String id, @RequestBody Conge conge) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de modifier des conges (lecture seule)");
        }
        return service.update(id, conge);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('LEAVE_REJECT')")
    public Conge cancel(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            throw new RuntimeException("TOP_MANAGER n'a pas la permission d'annuler des conges (lecture seule)");
        }
        return service.cancel(id);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    public Conge approve(@PathVariable String id, @RequestBody CongeValidationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        if (isTopManager(auth)) {
            log.warn("TOP_MANAGER {} - Tentative d'approbation refusee (lecture seule)", userId);
            throw new RuntimeException("TOP_MANAGER n'a pas la permission d'approuver des conges (lecture seule)");
        }

        log.info("Tentative d'approbation par l'utilisateur: {}", userId);

        Employee manager = findEmployeeByUserId(userId);
        if (manager == null) {
            log.error("Aucun employe trouve pour l'utilisateur: {}", userId);
            throw new RuntimeException("Vous n'etes pas enregistre comme employe dans le systeme. Contactez l'administrateur.");
        }

        log.info("Manager trouve: {} {} (ID: {}, Departement: {})",
                manager.getPrenom(), manager.getNom(), manager.getId(), manager.getDepartementId());

        Conge conge = service.getById(id);
        Employee employee = conge.getEmployee();

        log.info("Conge demande par: {} {} (Departement: {})",
                employee.getPrenom(), employee.getNom(), employee.getDepartementId());

        if (manager.getDepartementId() == null || manager.getDepartementId().isEmpty()) {
            throw new RuntimeException("Votre departement n'est pas defini. Contactez l'administrateur.");
        }

        if (employee.getDepartementId() == null || employee.getDepartementId().isEmpty()) {
            throw new RuntimeException("L'employe n'a pas de departement defini. Contactez l'administrateur.");
        }

        if (!manager.getDepartementId().equals(employee.getDepartementId())) {
            log.warn("Tentative d'approbation interdite! Manager {} (dept: {}) vs Employe {} (dept: {})",
                    manager.getNomComplet(), manager.getDepartementId(),
                    employee.getNomComplet(), employee.getDepartementId());
            throw new RuntimeException("Vous ne pouvez pas approuver les conges d'un autre departement");
        }

        log.info("Approbation autorisee - Meme departement: {}", manager.getDepartementId());

        return service.approve(id, manager.getId(), request.getCommentaire());
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE_REJECT')")
    public Conge reject(@PathVariable String id, @RequestBody CongeValidationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        if (isTopManager(auth)) {
            log.warn("TOP_MANAGER {} - Tentative de rejet refusee (lecture seule)", userId);
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de rejeter des conges (lecture seule)");
        }

        log.info("Tentative de rejet par l'utilisateur: {}", userId);

        Employee manager = findEmployeeByUserId(userId);
        if (manager == null) {
            log.error("Aucun employe trouve pour l'utilisateur: {}", userId);
            throw new RuntimeException("Vous n'etes pas enregistre comme employe dans le systeme. Contactez l'administrateur.");
        }

        log.info("Manager trouve: {} {} (ID: {}, Departement: {})",
                manager.getPrenom(), manager.getNom(), manager.getId(), manager.getDepartementId());

        Conge conge = service.getById(id);
        Employee employee = conge.getEmployee();

        log.info("Conge demande par: {} {} (Departement: {})",
                employee.getPrenom(), employee.getNom(), employee.getDepartementId());

        if (manager.getDepartementId() == null || manager.getDepartementId().isEmpty()) {
            throw new RuntimeException("Votre departement n'est pas defini. Contactez l'administrateur.");
        }

        if (employee.getDepartementId() == null || employee.getDepartementId().isEmpty()) {
            throw new RuntimeException("L'employe n'a pas de departement defini. Contactez l'administrateur.");
        }

        if (!manager.getDepartementId().equals(employee.getDepartementId())) {
            log.warn("Tentative de rejet interdite! Manager {} (dept: {}) vs Employe {} (dept: {})",
                    manager.getNomComplet(), manager.getDepartementId(),
                    employee.getNomComplet(), employee.getDepartementId());
            throw new RuntimeException("Vous ne pouvez pas rejeter les conges d'un autre departement");
        }

        log.info("Rejet autorise - Meme departement: {}", manager.getDepartementId());

        return service.reject(id, manager.getId(), request.getCommentaire());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_OWN')")
    public List<Conge> getByEmployee(@PathVariable String employeeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        // Admin/TOP_MANAGER peuvent tout voir
        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Acces aux conges de l'employe {}", employeeId);
            return service.getByEmployee(employeeId);
        }

        // Récupérer l'employé associé à l'utilisateur courant
        Employee currentEmployee = findEmployeeByUserId(userId);

        // L'utilisateur peut voir ses propres congés si l'employeeId correspond à son employé
        if (currentEmployee != null && currentEmployee.getId().equals(employeeId)) {
            log.info("Employe {} - Acces a ses propres conges", userId);
            return service.getByEmployee(employeeId);
        }

        // Sinon, c'est un manager essayant de voir les congés d'un autre employé
        if (currentEmployee == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return List.of();
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            log.warn("Employe non trouve avec l'ID: {}", employeeId);
            return List.of();
        }

        Employee targetEmployee = employeeOpt.get();

        // Vérifier que le manager a un département et qu'il correspond à celui de l'employé ciblé
        if (currentEmployee.getDepartementId() == null || currentEmployee.getDepartementId().isEmpty()) {
            log.warn("Le manager {} n'a pas de departement assigne", userId);
            return List.of();
        }

        if (currentEmployee.getDepartementId().equals(targetEmployee.getDepartementId())) {
            log.info("Manager {} - Acces aux conges de l'employe {} du meme departement", userId, employeeId);
            return service.getByEmployee(employeeId);
        }

        log.warn("Manager {} - Tentative d'acces aux conges d'un autre departement: {}", userId, employeeId);
        return List.of();
    }

    @GetMapping("/employee/{employeeId}/year/{annee}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_OWN')")
    public List<Conge> getByEmployeeAndYear(@PathVariable String employeeId, @PathVariable Integer annee) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Acces aux conges de l'employe {} pour l'annee {}", employeeId, annee);
            return service.getByEmployeeAndYear(employeeId, annee);
        }

        Employee currentEmployee = findEmployeeByUserId(userId);

        // L'utilisateur peut voir ses propres congés
        if (currentEmployee != null && currentEmployee.getId().equals(employeeId)) {
            return service.getByEmployeeAndYear(employeeId, annee);
        }

        if (currentEmployee == null) {
            return List.of();
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty() || currentEmployee.getDepartementId() == null) {
            return List.of();
        }

        if (!currentEmployee.getDepartementId().equals(employeeOpt.get().getDepartementId())) {
            log.warn("Manager {} - Tentative d'acces aux conges d'un autre departement", userId);
            return List.of();
        }

        return service.getByEmployeeAndYear(employeeId, annee);
    }

    @GetMapping("/employee/{employeeId}/pending")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_OWN')")
    public List<Conge> getPendingLeaves(@PathVariable String employeeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        boolean isRealAdmin = isAdmin(auth);
        boolean isTopManager = isTopManager(auth);

        if (isRealAdmin || isTopManager) {
            log.info("Admin/TOP_MANAGER - Acces aux conges en attente de l'employe {}", employeeId);
            return service.getPendingLeaves(employeeId);
        }

        Employee currentEmployee = findEmployeeByUserId(userId);

        // L'utilisateur peut voir ses propres congés en attente
        if (currentEmployee != null && currentEmployee.getId().equals(employeeId)) {
            return service.getPendingLeaves(employeeId);
        }

        if (currentEmployee == null) {
            return List.of();
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty() || currentEmployee.getDepartementId() == null) {
            return List.of();
        }

        if (!currentEmployee.getDepartementId().equals(employeeOpt.get().getDepartementId())) {
            log.warn("Manager {} - Tentative d'acces aux conges d'un autre departement", userId);
            return List.of();
        }

        return service.getPendingLeaves(employeeId);
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL')")
    public List<Conge> getByStatut(@PathVariable StatutConge statut) {
        return service.getByStatut(statut);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL')")
    public List<Conge> getByType(@PathVariable TypeConge type) {
        return service.getByType(type);
    }

    @GetMapping("/employee/{employeeId}/solde-annuel")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL') or hasAuthority('LEAVE_VIEW_OWN')")
    public ResponseEntity<Integer> getSoldeAnnuelRestant(@PathVariable String employeeId) {
        int solde = service.calculerSoldeRestant(employeeId);
        log.info("Solde restant pour {}: {}", employeeId, solde);
        return ResponseEntity.ok(solde);
    }

    @GetMapping("/employee/{employeeId}/droits-annuels")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_ALL') or hasAuthority('LEAVE_VIEW_OWN')")
    public ResponseEntity<Integer> getDroitsAnnuels(@PathVariable String employeeId) {
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe introuvable avec l'ID : " + employeeId));
        int droits = service.calculerDroitsAnnuels(employee);
        log.info("Droits annuels pour {}: {}", employeeId, droits);
        return ResponseEntity.ok(droits);
    }

    @PatchMapping("/employee/{employeeId}/update-enfants")
    @PreAuthorize("hasAuthority('LEAVE_UPDATE') or hasAuthority('RH')")
    public ResponseEntity<Employee> updateEnfants(
            @PathVariable String employeeId,
            @RequestParam Integer nombreEnfants) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTopManager(auth)) {
            log.warn("TOP_MANAGER - Tentative de modification du nombre d'enfants refusee (lecture seule)");
            throw new RuntimeException("TOP_MANAGER n'a pas la permission de modifier les donnees des employes (lecture seule)");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe non trouve"));
        employee.setNombreEnfantsMoinsDe7Ans(nombreEnfants);
        employee.setUpdatedAt(LocalDate.now());
        employeeRepository.save(employee);
        log.info("Nombre d'enfants mis a jour pour {}: {}", employeeId, nombreEnfants);
        return ResponseEntity.ok(employee);
    }
}