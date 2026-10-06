package com.fric.sirh.service;

import com.fric.sirh.model.Entreprise;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.EntrepriseRepository;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository repo;
    private final DepartementRepository departementRepo;
    private final EmployeeRepository employeeRepo;
    private final NotificationPublisher notificationPublisher;

    public Entreprise create(Entreprise entreprise) {
        entreprise.setCreatedAt(LocalDate.now());
        if (entreprise.getStatut() == null || entreprise.getStatut().trim().isEmpty()) {
            entreprise.setStatut("ACTIF");
        } else {
            entreprise.setStatut(entreprise.getStatut().toUpperCase().trim());
        }
        log.info("Entreprise creee: {}", entreprise.getName());
        Entreprise saved = repo.save(entreprise);

        // Récupérer l'utilisateur authentifié
        String userId = getCurrentUserId();
        publishCompanyEvent(saved, "CREATE", userId);

        return saved;
    }

    public List<Entreprise> getAll() {
        return repo.findAll();
    }

    public Entreprise getById(String id) {
        return repo.findById(id).orElse(null);
    }

    public Entreprise getEntrepriseByEmployeeId(String employeeId) {
        log.info("Recherche de l'entreprise pour l'employe ID: {}", employeeId);

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe non trouve avec ID: " + employeeId));

        String departementId = employee.getDepartementId();
        if (departementId == null || departementId.isBlank()) {
            log.warn("L'employe {} n'a pas de departement assigne", employeeId);
            return null;
        }

        Departement departement = departementRepo.findById(departementId)
                .orElseThrow(() -> new RuntimeException("Departement non trouve avec ID: " + departementId));

        String entrepriseId = departement.getEntrepriseId();
        if (entrepriseId == null || entrepriseId.isBlank()) {
            log.warn("Le departement {} n'a pas d'entreprise assignee", departementId);
            return null;
        }

        Entreprise entreprise = repo.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
            log.info("Entreprise trouvee: {}", entreprise.getName());
        } else {
            log.warn("Entreprise non trouvee avec ID: {}", entrepriseId);
        }
        return entreprise;
    }

    public Entreprise update(String id, Entreprise entreprise) {
        return repo.findById(id).map(ent -> {
            ent.setName(entreprise.getName());
            ent.setSiege(entreprise.getSiege());
            ent.setAdresse(entreprise.getAdresse());
            ent.setTelephone(entreprise.getTelephone());
            ent.setEmail(entreprise.getEmail());
            if (entreprise.getStatut() != null && !entreprise.getStatut().trim().isEmpty()) {
                ent.setStatut(entreprise.getStatut().toUpperCase().trim());
            }
            ent.setUpdatedBy(entreprise.getUpdatedBy());
            ent.setUpdatedAt(LocalDate.now());
            log.info("Entreprise mise a jour: {}", ent.getName());
            Entreprise saved = repo.save(ent);

            // Récupérer l'utilisateur authentifié pour la notification
            String userId = getCurrentUserId();
            publishCompanyEvent(saved, "UPDATE", userId);

            return saved;
        }).orElse(null);
    }

    public Entreprise suspendre(String id) {
        return repo.findById(id).map(ent -> {
            ent.setStatut("SUSPENDU");
            ent.setUpdatedAt(LocalDate.now());
            log.info("Entreprise suspendue: {}", ent.getName());
            Entreprise saved = repo.save(ent);

            String userId = getCurrentUserId();
            publishCompanyEvent(saved, "SUSPEND", userId);

            return saved;
        }).orElse(null);
    }

    public Entreprise reactiver(String id) {
        return repo.findById(id).map(ent -> {
            ent.setStatut("ACTIF");
            ent.setUpdatedAt(LocalDate.now());
            log.info("Entreprise reactivee: {}", ent.getName());
            Entreprise saved = repo.save(ent);
            // Pas de notification pour reactivation
            return saved;
        }).orElse(null);
    }

    public void delete(String id) {
        Entreprise entreprise = getById(id);
        if (entreprise == null) return;
        repo.deleteById(id);
        log.info("Entreprise supprimee avec ID: {}", id);

        String userId = getCurrentUserId();
        publishCompanyEvent(entreprise, "DELETE", userId);
    }

    // ==========================================
    // MÉTHODES PRIVÉES
    // ==========================================

    private String getCurrentUserId() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'utilisateur authentifie, fallback a SYSTEM", e);
            return "SYSTEM";
        }
    }

    private void publishCompanyEvent(Entreprise entreprise, String action, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("companyId", entreprise.getId());
        data.put("entityId", entreprise.getId());
        data.put("entityType", "ENTREPRISE");
        data.put("actionUrl", "/entreprises/" + entreprise.getId());
        data.put("companyName", entreprise.getName()); // Ajout du nom pour le dispatcher
        data.put("metadata", Map.of(
                "name", entreprise.getName(),
                "statut", entreprise.getStatut(),
                "action", action
        ));

        NotificationEvent event = switch (action) {
            case "CREATE" -> NotificationEvent.COMPANY_CREATED;
            case "UPDATE" -> NotificationEvent.COMPANY_UPDATED;
            case "DELETE", "SUSPEND" -> NotificationEvent.COMPANY_DELETED;
            default -> NotificationEvent.SYSTEM;
        };

        notificationPublisher.publish(event, data, userId != null ? userId : "SYSTEM");
    }
}