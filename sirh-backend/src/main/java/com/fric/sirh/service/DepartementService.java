package com.fric.sirh.service;

import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.EntrepriseRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DepartementService {

    private final DepartementRepository departementRepo;
    private final EmployeeRepository employeeRepo;
    private final EntrepriseRepository entrepriseRepo;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    // ==========================================
    // MÉTHODE PRIVÉE POUR RÉCUPÉRER L'UTILISATEUR AUTHENTIFIÉ
    // ==========================================

    private String getCurrentUserId() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'utilisateur authentifie, fallback a SYSTEM", e);
            return "SYSTEM";
        }
    }

    // ==========================================
    // Récupérer le département par USER ID
    // ==========================================

    public Departement getDepartementByUserId(String userId) {
        log.info("Recherche du departement pour l'utilisateur ID: {}", userId);

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("Utilisateur non trouve avec ID: {}", userId);
                return null;
            }

            User user = userOpt.get();
            String employeeId = user.getEmployeeId();
            if (employeeId == null || employeeId.isBlank()) {
                log.warn("L'utilisateur {} n'a pas d'employeeId associe", userId);
                return null;
            }

            Optional<Employee> employeeOpt = employeeRepo.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                log.warn("Employe non trouve avec ID: {}", employeeId);
                return null;
            }

            Employee employee = employeeOpt.get();
            String departementId = employee.getDepartementId();
            if (departementId == null || departementId.isBlank()) {
                log.warn("L'employe {} n'a pas de departement assigne", employeeId);
                return null;
            }

            Optional<Departement> deptOpt = departementRepo.findById(departementId);
            if (deptOpt.isEmpty()) {
                log.warn("Departement non trouve avec ID: {}", departementId);
                return null;
            }

            log.info("Departement trouve: {} (ID: {})", deptOpt.get().getName(), departementId);
            return deptOpt.get();

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation du departement: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==========================================
    // FILTRAGE PAR ENTREPRISE
    // ==========================================

    public List<Departement> getDepartementsByUserCompany(String userId) {
        log.info("Recuperation des departements pour l'utilisateur: {}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId null ou vide");
            return List.of();
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("Utilisateur non trouve avec l'ID: {}", userId);
                return List.of();
            }

            User user = userOpt.get();
            String employeeId = user.getEmployeeId();
            if (employeeId == null || employeeId.trim().isEmpty()) {
                log.warn("L'utilisateur {} n'a pas d'employeeId associe", userId);
                return List.of();
            }

            Optional<Employee> employeeOpt = employeeRepo.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                log.warn("Employe non trouve avec l'ID: {}", employeeId);
                return List.of();
            }

            Employee employee = employeeOpt.get();
            String departementId = employee.getDepartementId();
            if (departementId == null || departementId.trim().isEmpty()) {
                log.warn("L'employe {} n'a pas de departement assigne", employeeId);
                return List.of();
            }

            Optional<Departement> deptOpt = departementRepo.findById(departementId);
            if (deptOpt.isEmpty()) {
                log.warn("Departement non trouve avec l'ID: {}", departementId);
                return List.of();
            }

            String entrepriseId = deptOpt.get().getEntrepriseId();
            if (entrepriseId == null || entrepriseId.trim().isEmpty()) {
                log.warn("Le departement {} n'a pas d'entreprise associee", departementId);
                return List.of();
            }

            List<Departement> departements = departementRepo.findByEntrepriseId(entrepriseId);
            log.info("{} departements trouves pour l'entreprise {} (utilisateur {})",
                    departements.size(), entrepriseId, userId);

            return departements;

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des departements: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public boolean isDepartementInUserCompany(String userId, String departementId) {
        if (userId == null || departementId == null) {
            return false;
        }

        try {
            List<Departement> userDepartements = getDepartementsByUserCompany(userId);
            return userDepartements.stream()
                    .anyMatch(d -> d.getId().equals(departementId));
        } catch (Exception e) {
            log.error("Erreur lors de la verification: {}", e.getMessage());
            return false;
        }
    }

    public String getCompanyIdByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return null;
            }

            User user = userOpt.get();
            String employeeId = user.getEmployeeId();
            if (employeeId == null || employeeId.trim().isEmpty()) {
                return null;
            }

            Optional<Employee> employeeOpt = employeeRepo.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                return null;
            }

            Employee employee = employeeOpt.get();
            String departementId = employee.getDepartementId();
            if (departementId == null || departementId.trim().isEmpty()) {
                return null;
            }

            Optional<Departement> deptOpt = departementRepo.findById(departementId);
            return deptOpt.map(Departement::getEntrepriseId).orElse(null);

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation de l'entreprise: {}", e.getMessage());
            return null;
        }
    }

    // ==========================================
    // CRUD
    // ==========================================

    public Departement create(Departement departement) {
        if (departement.getEntrepriseId() == null || departement.getEntrepriseId().isBlank()) {
            throw new RuntimeException("Entreprise obligatoire");
        }
        entrepriseRepo.findById(departement.getEntrepriseId())
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        departement.setCreatedAt(LocalDate.now());
        if (departement.getStatut() == null || departement.getStatut().isBlank()) {
            departement.setStatut("ACTIF");
        }

        log.info("Departement cree: {}", departement.getName());
        Departement saved = departementRepo.save(departement);

        String userId = getCurrentUserId();
        publishDepartmentEvent(saved, "CREATE", userId);

        return saved;
    }

    public List<Departement> getAll() {
        return departementRepo.findAll();
    }

    public Departement getById(String id) {
        return departementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement introuvable"));
    }

    public Departement getDepartementByEmployeeId(String employeeId) {
        log.info("Recherche du departement pour l'employe ID: {}", employeeId);

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe non trouve avec ID: " + employeeId));

        String departementId = employee.getDepartementId();
        if (departementId == null || departementId.isBlank()) {
            log.warn("L'employe {} n'a pas de departement assigne", employeeId);
            return null;
        }

        Departement departement = departementRepo.findById(departementId).orElse(null);
        if (departement != null) {
            log.info("Departement trouve: {}", departement.getName());
        } else {
            log.warn("Departement non trouve avec ID: {}", departementId);
        }
        return departement;
    }

    public Departement update(String id, Departement d) {
        Departement dep = departementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement introuvable"));

        dep.setName(d.getName());

        if (d.getEntrepriseId() != null && !d.getEntrepriseId().isBlank()) {
            entrepriseRepo.findById(d.getEntrepriseId())
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
            dep.setEntrepriseId(d.getEntrepriseId());
        }

        dep.setUpdatedAt(LocalDate.now());
        dep.setUpdatedBy(d.getUpdatedBy());

        log.info("Departement mis a jour: {}", dep.getName());
        Departement saved = departementRepo.save(dep);

        String userId = getCurrentUserId();
        publishDepartmentEvent(saved, "UPDATE", userId);

        return saved;
    }

    public Departement suspendre(String id) {
        Departement dep = departementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement introuvable"));

        dep.setStatut("SUSPENDU");
        dep.setUpdatedAt(LocalDate.now());

        log.info("Departement suspendu: {}", dep.getName());
        Departement saved = departementRepo.save(dep);

        String userId = getCurrentUserId();
        publishDepartmentEvent(saved, "SUSPEND", userId);

        return saved;
    }

    public Departement reactiver(String id) {
        Departement dep = departementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Departement introuvable"));

        dep.setStatut("ACTIF");
        dep.setUpdatedAt(LocalDate.now());

        log.info("Departement reactive: {}", dep.getName());
        Departement saved = departementRepo.save(dep);

        // Pas de notification pour réactivation (ou on peut en ajouter une si besoin)
        return saved;
    }

    public Employee affecterEmploye(String employeeId, String departementId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe introuvable"));

        departementRepo.findById(departementId)
                .orElseThrow(() -> new RuntimeException("Departement introuvable"));

        employee.setDepartementId(departementId);
        log.info("Employe {} affecte au departement {}", employeeId, departementId);
        return employeeRepo.save(employee);
    }

    public List<Departement> getByEntreprise(String entrepriseId) {
        entrepriseRepo.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        return departementRepo.findByEntrepriseId(entrepriseId);
    }

    // ==========================================
    // PUBLICATION DES NOTIFICATIONS
    // ==========================================

    private void publishDepartmentEvent(Departement departement, String action, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("companyId", departement.getEntrepriseId());
        data.put("departmentId", departement.getId());
        data.put("entityId", departement.getId());
        data.put("entityType", "DEPARTEMENT");
        data.put("actionUrl", "/departements/" + departement.getId());
        data.put("departmentName", departement.getName()); // Ajout pour le dispatcher

        // Récupérer le nom de l'entreprise
        if (departement.getEntrepriseId() != null) {
            entrepriseRepo.findById(departement.getEntrepriseId()).ifPresent(ent -> {
                data.put("companyName", ent.getName());
            });
        }

        data.put("metadata", Map.of(
                "name", departement.getName(),
                "statut", departement.getStatut(),
                "action", action
        ));

        NotificationEvent event = switch (action) {
            case "CREATE" -> NotificationEvent.DEPARTMENT_CREATED;
            case "UPDATE" -> NotificationEvent.DEPARTMENT_UPDATED;
            case "SUSPEND", "DELETE" -> NotificationEvent.DEPARTMENT_DELETED;
            default -> NotificationEvent.SYSTEM;
        };

        notificationPublisher.publish(event, data, userId != null ? userId : "SYSTEM");
    }
}