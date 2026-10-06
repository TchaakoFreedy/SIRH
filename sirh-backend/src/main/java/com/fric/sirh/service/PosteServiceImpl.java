package com.fric.sirh.service;

import com.fric.sirh.exception.DuplicateResourceException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Entreprise;
import com.fric.sirh.model.Poste;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.EntrepriseRepository;
import com.fric.sirh.repository.PosteRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PosteServiceImpl implements PosteService {

    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final NotificationPublisher notificationPublisher;

    public PosteServiceImpl(PosteRepository posteRepository,
                            DepartementRepository departementRepository,
                            UserRepository userRepository,
                            EmployeeRepository employeeRepository,
                            EntrepriseRepository entrepriseRepository,
                            NotificationPublisher notificationPublisher) {
        this.posteRepository = posteRepository;
        this.departementRepository = departementRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.notificationPublisher = notificationPublisher;
    }

    // ==========================================
    // ✅ MÉTHODE DE FILTRAGE POUR TOUS LES RÔLES
    // ==========================================

    @Override
    public List<Poste> getPostesByUserCompany(String userId) {
        log.info("Recuperation des postes pour l'utilisateur: {}", userId);

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
            log.info("Utilisateur trouve: {} - employeeId: {}", user.getEmail(), user.getEmployeeId());

            String employeeId = user.getEmployeeId();
            if (employeeId == null || employeeId.trim().isEmpty()) {
                log.warn("L'utilisateur {} n'a pas d'employeeId associe", userId);
                return List.of();
            }

            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                log.warn("Employe non trouve avec l'ID: {}", employeeId);
                return List.of();
            }

            Employee employee = employeeOpt.get();
            log.info("Employe trouve: {} {} - departementId: {}",
                    employee.getPrenom(), employee.getNom(), employee.getDepartementId());

            String departementId = employee.getDepartementId();
            if (departementId == null || departementId.trim().isEmpty()) {
                log.warn("L'employe {} n'a pas de departement assigne", employeeId);
                return List.of();
            }

            Optional<Departement> deptOpt = departementRepository.findById(departementId);
            if (deptOpt.isEmpty()) {
                log.warn("Departement non trouve avec l'ID: {}", departementId);
                return List.of();
            }

            String entrepriseId = deptOpt.get().getEntrepriseId();
            if (entrepriseId == null || entrepriseId.trim().isEmpty()) {
                log.warn("Le departement {} n'a pas d'entreprise associee", departementId);
                return List.of();
            }

            log.info("Entreprise trouvee: {}", entrepriseId);

            List<Departement> departements = departementRepository.findByEntrepriseId(entrepriseId);
            if (departements.isEmpty()) {
                log.warn("Aucun departement trouve pour l'entreprise {}", entrepriseId);
                return List.of();
            }

            log.info("{} departements trouves pour l'entreprise {}", departements.size(), entrepriseId);
            for (Departement d : departements) {
                log.info("   - Departement: {} (ID: {})", d.getName(), d.getId());
            }

            List<String> departementIds = departements.stream()
                    .map(Departement::getId)
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();

            if (departementIds.isEmpty()) {
                log.warn("Aucun ID de departement valide pour l'entreprise {}", entrepriseId);
                return List.of();
            }

            log.info("IDs des departements: {}", departementIds);

            List<Poste> allPostes = posteRepository.findAll();
            log.info("   Total postes en base: {}", allPostes.size());

            for (Poste p : allPostes) {
                String deptId = p.getDepartement() != null ? p.getDepartement().getId() : "null";
                String deptName = p.getDepartement() != null ? p.getDepartement().getName() : "null";
                log.info("   Poste: {} (Code: {}, Departement ID: {}, Departement Nom: {})",
                        p.getLibelle(), p.getCode(), deptId, deptName);
            }

            List<Poste> postes = allPostes.stream()
                    .filter(p -> {
                        if (p.getDepartement() == null) {
                            return false;
                        }
                        String deptId = p.getDepartement().getId();
                        return deptId != null && departementIds.contains(deptId);
                    })
                    .toList();

            log.info("{} postes trouves pour l'entreprise {} (utilisateur {})",
                    postes.size(), entrepriseId, userId);

            for (Poste p : postes) {
                log.info("   - Poste trouve: {} (Code: {}, Departement: {})",
                        p.getLibelle(), p.getCode(),
                        p.getDepartement() != null ? p.getDepartement().getName() : "null");
            }

            return postes;

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des postes: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<Poste> getActivePostesByUserCompany(String userId) {
        log.info("Recuperation des postes actifs pour l'utilisateur: {}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            return List.of();
        }

        try {
            List<Poste> allPostes = getPostesByUserCompany(userId);
            return allPostes.stream()
                    .filter(Poste::isActive)
                    .toList();

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des postes actifs: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isPosteInUserCompany(String userId, String posteId) {
        if (userId == null || posteId == null) {
            return false;
        }

        try {
            Optional<Poste> posteOpt = posteRepository.findById(posteId);
            if (posteOpt.isEmpty()) {
                return false;
            }

            Poste poste = posteOpt.get();
            Departement departement = poste.getDepartement();

            if (departement == null || departement.getId() == null) {
                return false;
            }

            Optional<Departement> deptOpt = departementRepository.findById(departement.getId());
            if (deptOpt.isEmpty()) {
                return false;
            }

            String posteEntrepriseId = deptOpt.get().getEntrepriseId();
            if (posteEntrepriseId == null || posteEntrepriseId.trim().isEmpty()) {
                return false;
            }

            String userEntrepriseId = getCompanyIdByUserId(userId);
            if (userEntrepriseId == null) {
                return false;
            }

            return posteEntrepriseId.equals(userEntrepriseId);

        } catch (Exception e) {
            log.error("Erreur lors de la verification: {}", e.getMessage());
            return false;
        }
    }

    @Override
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

            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
            if (employeeOpt.isEmpty()) {
                return null;
            }

            Employee employee = employeeOpt.get();
            String departementId = employee.getDepartementId();
            if (departementId == null || departementId.trim().isEmpty()) {
                return null;
            }

            Optional<Departement> deptOpt = departementRepository.findById(departementId);
            return deptOpt.map(Departement::getEntrepriseId).orElse(null);

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation de l'entreprise: {}", e.getMessage());
            return null;
        }
    }

    // ==========================================
    // ✅ MÉTHODES EXISTANTES
    // ==========================================

    @Override
    public Poste create(Poste poste) {
        if (posteRepository.existsByCode(poste.getCode())) {
            throw new DuplicateResourceException("Poste", "code", poste.getCode());
        }

        if (poste.getDepartement() != null && poste.getDepartement().getId() != null) {
            Departement dept = departementRepository.findById(poste.getDepartement().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement", poste.getDepartement().getId()));
            poste.setDepartement(dept);
            log.info("Poste associe au departement: {} (ID: {})", dept.getName(), dept.getId());
        } else {
            log.warn("Poste cree sans departement");
        }

        poste.setId(null);
        LocalDate now = LocalDate.now();
        poste.setCreatedAt(now);
        poste.setUpdatedAt(now);
        poste.setActive(true);

        if (poste.getCreatedBy() == null || poste.getCreatedBy().isEmpty()) {
            poste.setCreatedBy("SYSTEM");
        }

        Poste saved = posteRepository.save(poste);
        log.info("Poste cree avec succes: {} - {}", saved.getCode(), saved.getLibelle());

        publishPositionEvent(saved, "CREATE");
        return saved;
    }

    @Override
    public List<Poste> getAll() {
        List<Poste> postes = posteRepository.findAll();
        log.info("Total des postes en base: {}", postes.size());
        return postes;
    }

    @Override
    public List<Poste> getActivePostes() {
        return posteRepository.findByActiveTrue();
    }

    @Override
    public Optional<Poste> getById(String id) {
        return posteRepository.findById(id);
    }

    @Override
    public Poste update(String id, Poste updatedPoste) {
        Poste existingPoste = posteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste", id));

        if (!existingPoste.getCode().equals(updatedPoste.getCode()) &&
                posteRepository.existsByCode(updatedPoste.getCode())) {
            throw new DuplicateResourceException("Poste", "code", updatedPoste.getCode());
        }

        if (updatedPoste.getDepartement() != null && updatedPoste.getDepartement().getId() != null) {
            Departement dept = departementRepository.findById(updatedPoste.getDepartement().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement", updatedPoste.getDepartement().getId()));
            existingPoste.setDepartement(dept);
        }

        existingPoste.setCode(updatedPoste.getCode());
        existingPoste.setLibelle(updatedPoste.getLibelle());
        existingPoste.setDescription(updatedPoste.getDescription());
        existingPoste.setUpdatedAt(LocalDate.now());

        if (updatedPoste.getUpdatedBy() != null && !updatedPoste.getUpdatedBy().isEmpty()) {
            existingPoste.setUpdatedBy(updatedPoste.getUpdatedBy());
        }

        Poste saved = posteRepository.save(existingPoste);
        log.info("Poste mis a jour avec succes: {} - {}", saved.getCode(), saved.getLibelle());

        publishPositionEvent(saved, "UPDATE");
        return saved;
    }

    @Override
    public void delete(String id) {
        if (!posteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Poste", id);
        }
        Poste poste = posteRepository.findById(id).orElseThrow();
        posteRepository.deleteById(id);
        log.info("Poste supprime avec succes: ID {}", id);

        publishPositionEvent(poste, "DELETE");
    }

    @Override
    public Poste toggleActive(String id) {
        Poste poste = posteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste", id));

        boolean newStatus = !poste.isActive();
        poste.setActive(newStatus);
        poste.setUpdatedAt(LocalDate.now());

        Poste saved = posteRepository.save(poste);
        log.info("Statut du poste {} change vers: {}", id, newStatus ? "ACTIF" : "INACTIF");

        if (!newStatus) {
            publishPositionEvent(saved, "DELETE");
        }

        return saved;
    }

    @Override
    public List<Poste> getByDepartement(String departementId) {
        log.info("Recuperation des postes pour le departement: {}", departementId);
        List<Poste> postes = posteRepository.findByDepartementId(departementId);
        log.info("{} postes trouves pour le departement", postes.size());
        return postes;
    }

    // ==========================================
    // ✅ MÉTHODE DE PUBLICATION DES NOTIFICATIONS (CORRIGÉE)
    // ==========================================

    private void publishPositionEvent(Poste poste, String action) {
        String userId = "SYSTEM";
        try {
            userId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Utilisateur connecte pour la notification : {}", userId);
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'utilisateur authentifie, fallback a SYSTEM", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("companyId", poste.getDepartement() != null ? poste.getDepartement().getEntrepriseId() : null);
        data.put("departmentId", poste.getDepartement() != null ? poste.getDepartement().getId() : null);
        data.put("entityId", poste.getId());
        data.put("entityType", "POSTE");
        data.put("actionUrl", "/postes/" + poste.getId());
        data.put("libelle", poste.getLibelle());
        data.put("code", poste.getCode());

        // ✅ Récupérer et ajouter le nom du département et de l'entreprise
        if (poste.getDepartement() != null && poste.getDepartement().getId() != null) {
            Optional<Departement> deptOpt = departementRepository.findById(poste.getDepartement().getId());
            if (deptOpt.isPresent()) {
                Departement dept = deptOpt.get();
                data.put("departmentName", dept.getName());
                String entrepriseId = dept.getEntrepriseId();
                if (entrepriseId != null) {
                    entrepriseRepository.findById(entrepriseId).ifPresent(ent -> {
                        data.put("companyName", ent.getName());
                    });
                }
            }
        } else {
            // Si le poste n'a pas de département, on met un nom par défaut
            data.put("departmentName", "Aucun département");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("code", poste.getCode());
        metadata.put("libelle", poste.getLibelle());
        metadata.put("action", action);
        data.put("metadata", metadata);

        NotificationEvent event = switch (action) {
            case "CREATE" -> NotificationEvent.POSITION_CREATED;
            case "UPDATE" -> NotificationEvent.POSITION_UPDATED;
            case "DELETE" -> NotificationEvent.POSITION_DELETED;
            default -> NotificationEvent.SYSTEM;
        };

        notificationPublisher.publish(event, data, userId);
    }
}