// com.fric.sirh.discipline.service.DisciplineService
package com.fric.sirh.discipline.service;

import com.fric.sirh.discipline.dto.*;
import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import com.fric.sirh.discipline.enums.TypeActionHistorique;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.model.HistoriqueDiscipline;
import com.fric.sirh.discipline.model.ReponseExplication;
import com.fric.sirh.discipline.repository.DemandeExplicationRepository;
import com.fric.sirh.discipline.repository.ReponseExplicationRepository;
import com.fric.sirh.discipline.mapper.DemandeExplicationMapper;
import com.fric.sirh.discipline.mapper.ReponseExplicationMapper;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.notification.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class DisciplineService {

    private final DemandeExplicationRepository demandeRepository;
    private final ReponseExplicationRepository reponseRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DemandeExplicationMapper demandeMapper;
    private final ReponseExplicationMapper reponseMapper;
    private final SecurityUtils securityUtils;

    public DisciplineService(DemandeExplicationRepository demandeRepository,
                             ReponseExplicationRepository reponseRepository,
                             EmployeeRepository employeeRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             DemandeExplicationMapper demandeMapper,
                             ReponseExplicationMapper reponseMapper,
                             SecurityUtils securityUtils) {
        this.demandeRepository = demandeRepository;
        this.reponseRepository = reponseRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.demandeMapper = demandeMapper;
        this.reponseMapper = reponseMapper;
        this.securityUtils = securityUtils;
    }

    // ==================== DEMANDES D'EXPLICATION ====================

    /**
     * Récupère toutes les demandes avec filtres selon le rôle de l'utilisateur
     * - RH et TOP_MANAGER : VOIR TOUTES les demandes
     * - DIRECTION : VOIR les demandes de SON ENTREPRISE uniquement
     * - MANAGER : VOIR les demandes de SON DÉPARTEMENT uniquement
     * - EMPLOYEE : VOIR SES PROPRES demandes uniquement
     */
    public Page<DemandeExplicationDTO> getAllDemandes(DisciplineFilterDTO filter, Pageable pageable) {
        String currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Role role = roleRepository.findById(currentUser.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé"));

        String roleName = role.getName();
        log.info("🔍 Rôle de l'utilisateur: {}", roleName);

        // Récupérer l'employé de l'utilisateur (si existe)
        Employee currentEmployee = null;
        if (currentUser.getEmployeeId() != null) {
            currentEmployee = employeeRepository.findById(currentUser.getEmployeeId())
                    .orElse(null);
        }

        Page<DemandeExplication> demandes;

        // ============================================
        // 1. RH et TOP_MANAGER : VOIR TOUTES les demandes
        // ============================================
        if ("RH".equals(roleName) || "TOP_MANAGER".equals(roleName)) {
            log.info("👤 {} - Accès à toutes les demandes", roleName);
            demandes = getDemandesWithFilters(filter, pageable);
        }
        // ============================================
        // 2. DIRECTION : VOIR les demandes de SON ENTREPRISE
        // ============================================
        else if ("DIRECTION".equals(roleName)) {
            if (currentEmployee == null) {
                log.error("❌ L'utilisateur DIRECTION n'a pas d'employé associé");
                throw new RuntimeException("Vous n'avez pas d'employé associé");
            }
            String entrepriseId = currentEmployee.getEntrepriseId();
            log.info("👤 DIRECTION - Accès aux demandes de l'entreprise: {}", entrepriseId);

            // Forcer le filtre sur l'entreprise
            if (filter == null) {
                filter = new DisciplineFilterDTO();
            }
            filter.setEntrepriseId(entrepriseId);
            demandes = getDemandesWithFilters(filter, pageable);
        }
        // ============================================
        // 3. MANAGER : VOIR les demandes de SON DÉPARTEMENT
        // ============================================
        else if ("MANAGER".equals(roleName)) {
            if (currentEmployee == null) {
                log.error("❌ L'utilisateur MANAGER n'a pas d'employé associé");
                throw new RuntimeException("Vous n'avez pas d'employé associé");
            }
            String departementId = currentEmployee.getDepartementId();
            log.info("👤 MANAGER - Accès aux demandes du département: {}", departementId);

            // Forcer le filtre sur le département
            if (filter == null) {
                filter = new DisciplineFilterDTO();
            }
            filter.setDepartementId(departementId);
            demandes = getDemandesWithFilters(filter, pageable);
        }
        // ============================================
        // 4. EMPLOYEE : VOIR SES PROPRES demandes
        // ============================================
        else if ("EMPLOYEE".equals(roleName)) {
            if (currentEmployee == null) {
                log.error("❌ L'utilisateur EMPLOYEE n'a pas d'employé associé");
                throw new RuntimeException("Vous n'avez pas d'employé associé");
            }
            String employeeId = currentEmployee.getId();
            log.info("👤 EMPLOYEE - Accès à ses propres demandes: {}", employeeId);

            // Forcer le filtre sur l'employé
            if (filter == null) {
                filter = new DisciplineFilterDTO();
            }
            filter.setEmployeId(employeeId);
            demandes = getDemandesWithFilters(filter, pageable);
        }
        // ============================================
        // 5. AUTRES RÔLES : Fallback - voir toutes les demandes
        // ============================================
        else {
            log.warn("⚠️ Rôle non reconnu: {}, accès à toutes les demandes", roleName);
            demandes = getDemandesWithFilters(filter, pageable);
        }

        return demandes.map(demandeMapper::toDTO);
    }

    /**
     * Méthode utilitaire pour appliquer les filtres sur les demandes
     */
    private Page<DemandeExplication> getDemandesWithFilters(DisciplineFilterDTO filter, Pageable pageable) {
        if (filter == null) {
            return demandeRepository.findAll(pageable);
        }

        if (filter.getEmployeId() != null) {
            return demandeRepository.findByEmployeConcerneId(filter.getEmployeId(), pageable);
        } else if (filter.getEntrepriseId() != null) {
            return demandeRepository.findByEntrepriseId(filter.getEntrepriseId(), pageable);
        } else if (filter.getDepartementId() != null) {
            return demandeRepository.findByDepartementId(filter.getDepartementId(), pageable);
        } else if (filter.getStatut() != null) {
            return demandeRepository.findByStatut(filter.getStatut(), pageable);
        } else if (filter.getDateDebut() != null && filter.getDateFin() != null) {
            return demandeRepository.findByDateCreationBetween(filter.getDateDebut(), filter.getDateFin(), pageable);
        } else if (filter.getAuteurId() != null) {
            return demandeRepository.findByAuteurId(filter.getAuteurId(), pageable);
        } else {
            return demandeRepository.findAll(pageable);
        }
    }

    /**
     * Récupère les demandes d'explication d'un employé spécifique
     * Un employé ne peut voir que ses propres demandes
     */
    @Transactional(readOnly = true)
    public Page<DemandeExplicationDTO> getDemandesByEmployee(String employeeId, Pageable pageable) {
        log.info("🔍 Récupération des demandes pour l'employé: {}", employeeId);

        // Vérifier que l'employé existe
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé avec l'id: " + employeeId));

        // Vérifier que l'utilisateur connecté est bien l'employé concerné
        String currentUserId = securityUtils.getCurrentUserId();
        if (!employee.getUserId().equals(currentUserId)) {
            log.warn("⚠️ Tentative d'accès aux demandes d'un autre employé par l'utilisateur {}", currentUserId);
            throw new RuntimeException("Vous ne pouvez voir que vos propres demandes");
        }

        Page<DemandeExplication> demandes = demandeRepository.findByEmployeConcerneId(employeeId, pageable);
        log.info("✅ {} demandes trouvées pour l'employé {}", demandes.getTotalElements(), employeeId);

        return demandes.map(demandeMapper::toDTO);
    }

    public DemandeExplicationDTO getDemandeById(String id) {
        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée avec l'id: " + id));
        return demandeMapper.toDTO(demande);
    }

    @Transactional
    public DemandeExplicationDTO createDemande(DemandeExplicationDTO dto, String userId) {
        log.info("🔍 Tentative de création d'une demande d'explication par l'utilisateur: {}", userId);

        User auteur = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur non trouvé: {}", userId);
                    return new RuntimeException("Utilisateur non trouvé");
                });

        log.info("📧 Utilisateur: {}", auteur.getEmail());
        log.info("🆔 Role ID: {}", auteur.getRoleId());

        Employee employe = employeeRepository.findById(dto.getEmployeConcerneId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        validateCreatePermission(auteur, employe);

        String numero = generateNumeroDemande();
        while (demandeRepository.existsByNumero(numero)) {
            numero = generateNumeroDemande();
        }

        DemandeExplication demande = new DemandeExplication();
        demande.setNumero(numero);
        demande.setObjet(dto.getObjet());
        demande.setDescription(dto.getDescription());
        demande.setMotif(dto.getMotif());
        demande.setEmployeConcerne(employe);
        demande.setAuteur(auteur);
        demande.setEntrepriseId(employe.getEntrepriseId());
        demande.setDepartementId(employe.getDepartementId());
        demande.setDateCreation(LocalDateTime.now());
        demande.setDateLimiteReponse(dto.getDateLimiteReponse());
        demande.setStatut(StatutDemandeExplication.EN_ATTENTE);
        demande.setCreatedBy(userId);
        demande.setCreatedAt(LocalDateTime.now());

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(auteur.getFullName())
                .action(TypeActionHistorique.DEMANDE_CREEE)
                .date(LocalDateTime.now())
                .commentaire("Demande d'explication créée: " + dto.getObjet())
                .build();
        demande.getHistorique().add(historique);

        DemandeExplication saved = demandeRepository.save(demande);
        log.info("✅ Demande d'explication créée avec le numéro: {}", saved.getNumero());

        return demandeMapper.toDTO(saved);
    }

    @Transactional
    public DemandeExplicationDTO updateDemande(String id, DemandeExplicationDTO dto, String userId) {
        DemandeExplication existing = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        if (existing.getStatut() == StatutDemandeExplication.VALIDEE ||
                existing.getStatut() == StatutDemandeExplication.REJETEE) {
            throw new RuntimeException("Impossible de modifier une demande déjà traitée");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        existing.setObjet(dto.getObjet());
        existing.setDescription(dto.getDescription());
        existing.setMotif(dto.getMotif());
        existing.setDateLimiteReponse(dto.getDateLimiteReponse());
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.DEMANDE_MODIFIEE)
                .date(LocalDateTime.now())
                .commentaire("Demande modifiée")
                .build();
        existing.getHistorique().add(historique);

        DemandeExplication updated = demandeRepository.save(existing);
        return demandeMapper.toDTO(updated);
    }

    @Transactional
    public DemandeExplicationDTO repondreDemande(String id, ReponseExplicationDTO dto, String userId) {
        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        if (!demande.getEmployeConcerne().getUserId().equals(userId)) {
            throw new RuntimeException("Seul l'employé concerné peut répondre à cette demande");
        }

        if (demande.getStatut() != StatutDemandeExplication.EN_ATTENTE) {
            throw new RuntimeException("Cette demande n'est plus en attente de réponse");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ReponseExplication reponse = new ReponseExplication();
        reponse.setDemandeExplication(demande);
        reponse.setContenu(dto.getContenu());
        reponse.setPiecesJointes(dto.getPiecesJointes());
        reponse.setDateReponse(LocalDateTime.now());
        reponse.setCreatedBy(userId);
        reponse.setCreatedAt(LocalDateTime.now());

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.EMPLOYE_A_REPONDU)
                .date(LocalDateTime.now())
                .commentaire("Réponse fournie: " + dto.getContenu())
                .build();
        demande.getHistorique().add(historique);

        demande.setStatut(StatutDemandeExplication.REPONDUE);
        demande.setReponse(reponse);
        demande.setUpdatedBy(userId);
        demande.setUpdatedAt(LocalDateTime.now());

        reponseRepository.save(reponse);
        DemandeExplication updated = demandeRepository.save(demande);

        return demandeMapper.toDTO(updated);
    }

    @Transactional
    public DemandeExplicationDTO marquerCommeRepondue(String id, String userId) {
        log.info("🔍 Tentative de marquer la demande {} comme répondue par l'utilisateur {}", id, userId);

        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        if (demande.getEmployeConcerne() == null || !demande.getEmployeConcerne().getUserId().equals(userId)) {
            log.error("❌ L'utilisateur {} n'est pas l'employé concerné par la demande {}", userId, id);
            throw new RuntimeException("Seul l'employé concerné peut marquer la demande comme répondue");
        }

        if (demande.getStatut() != StatutDemandeExplication.EN_ATTENTE) {
            log.warn("⚠️ La demande {} est en statut {} et non EN_ATTENTE", id, demande.getStatut());
            throw new RuntimeException("Seules les demandes en attente peuvent être marquées comme répondues");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ReponseExplication reponse = new ReponseExplication();
        reponse.setDemandeExplication(demande);
        reponse.setContenu("Réponse envoyée par email à l'auteur de la demande.");
        reponse.setDateReponse(LocalDateTime.now());
        reponse.setCreatedBy(userId);
        reponse.setCreatedAt(LocalDateTime.now());

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.EMPLOYE_A_REPONDU)
                .date(LocalDateTime.now())
                .commentaire("L'employé a répondu par email à la demande")
                .build();
        demande.getHistorique().add(historique);

        demande.setStatut(StatutDemandeExplication.REPONDUE);
        demande.setReponse(reponse);
        demande.setUpdatedBy(userId);
        demande.setUpdatedAt(LocalDateTime.now());

        reponseRepository.save(reponse);
        DemandeExplication updated = demandeRepository.save(demande);

        log.info("✅ Demande {} marquée comme répondue par l'employé {}", demande.getNumero(), userId);
        return demandeMapper.toDTO(updated);
    }

    @Transactional
    public DemandeExplicationDTO validerReponse(String id, String userId) {
        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        if (demande.getStatut() != StatutDemandeExplication.REPONDUE) {
            throw new RuntimeException("La réponse doit être fournie avant de pouvoir être validée");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.REPONSE_VALIDEE)
                .date(LocalDateTime.now())
                .commentaire("Réponse validée")
                .build();
        demande.getHistorique().add(historique);

        demande.setStatut(StatutDemandeExplication.VALIDEE);
        demande.setUpdatedBy(userId);
        demande.setUpdatedAt(LocalDateTime.now());

        DemandeExplication updated = demandeRepository.save(demande);
        return demandeMapper.toDTO(updated);
    }

    @Transactional
    public DemandeExplicationDTO rejeterReponse(String id, String userId) {
        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        if (demande.getStatut() != StatutDemandeExplication.REPONDUE) {
            throw new RuntimeException("La réponse doit être fournie avant de pouvoir être rejetée");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.REPONSE_REJETEE)
                .date(LocalDateTime.now())
                .commentaire("Réponse rejetée")
                .build();
        demande.getHistorique().add(historique);

        demande.setStatut(StatutDemandeExplication.REJETEE);
        demande.setUpdatedBy(userId);
        demande.setUpdatedAt(LocalDateTime.now());

        DemandeExplication updated = demandeRepository.save(demande);
        return demandeMapper.toDTO(updated);
    }

    @Transactional
    public void annulerDemande(String id, String userId) {
        DemandeExplication demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvée"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (demande.getStatut() == StatutDemandeExplication.VALIDEE ||
                demande.getStatut() == StatutDemandeExplication.REJETEE) {
            throw new RuntimeException("Impossible d'annuler une demande déjà traitée");
        }

        demande.setStatut(StatutDemandeExplication.ANNULEE);
        demande.setUpdatedBy(userId);
        demande.setUpdatedAt(LocalDateTime.now());

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(user.getFullName())
                .action(TypeActionHistorique.DEMANDE_MODIFIEE)
                .date(LocalDateTime.now())
                .commentaire("Demande annulée")
                .build();
        demande.getHistorique().add(historique);

        demandeRepository.save(demande);
        log.info("Demande d'explication annulée: {}", demande.getNumero());
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void validateCreatePermission(User auteur, Employee employe) {
        log.info("🔍 Validation des permissions pour l'utilisateur: {}", auteur.getId());
        log.info("📌 Role ID: {}", auteur.getRoleId());

        if (auteur.getRoleId() == null) {
            log.error("❌ L'utilisateur n'a pas de rôle!");
            throw new RuntimeException("Utilisateur sans rôle");
        }

        Role role = roleRepository.findById(auteur.getRoleId())
                .orElseThrow(() -> {
                    log.error("❌ Rôle non trouvé avec l'ID: {}", auteur.getRoleId());
                    return new RuntimeException("Rôle non trouvé");
                });

        String roleName = role.getName();
        log.info("📌 Nom du rôle: {}", roleName);

        // RH peut envoyer à n'importe quel employé
        if ("RH".equals(roleName)) {
            log.info("✅ Utilisateur RH - Permission accordée");
            return;
        }

        // DIRECTION peut envoyer aux employés de son entreprise
        if ("DIRECTION".equals(roleName)) {
            log.info("✅ Utilisateur DIRECTION - Vérification de l'entreprise");
            if (auteur.getEmployeeId() == null) {
                log.error("❌ L'utilisateur DIRECTION n'a pas d'employé associé");
                throw new RuntimeException("Vous n'avez pas d'employé associé");
            }
            Employee auteurEmploye = employeeRepository.findById(auteur.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employé de l'auteur non trouvé"));

            if (!auteurEmploye.getEntrepriseId().equals(employe.getEntrepriseId())) {
                log.error("❌ L'employé n'appartient pas à la même entreprise");
                throw new RuntimeException("La Direction ne peut envoyer une demande qu'aux employés de son entreprise");
            }
            log.info("✅ L'employé appartient à la même entreprise - Permission accordée");
            return;
        }

        // MANAGER peut envoyer aux employés de son département
        if ("MANAGER".equals(roleName)) {
            log.info("✅ Utilisateur MANAGER - Vérification du département");
            if (auteur.getEmployeeId() == null) {
                log.error("❌ L'utilisateur MANAGER n'a pas d'employé associé");
                throw new RuntimeException("Vous n'avez pas d'employé associé");
            }
            Employee auteurEmploye = employeeRepository.findById(auteur.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employé de l'auteur non trouvé"));

            if (!auteurEmploye.getDepartementId().equals(employe.getDepartementId())) {
                log.error("❌ L'employé n'appartient pas au même département");
                throw new RuntimeException("Le Manager ne peut envoyer une demande qu'aux employés de son département");
            }
            log.info("✅ L'employé appartient au même département - Permission accordée");
            return;
        }

        // EMPLOYEE ne peut pas créer de demande
        if ("EMPLOYEE".equals(roleName)) {
            log.error("❌ Un employé ne peut pas créer de demande d'explication");
            throw new RuntimeException("Les employés ne peuvent pas créer de demandes d'explication");
        }

        log.error("❌ Rôle non autorisé: {}", roleName);
        throw new RuntimeException("Vous n'avez pas la permission de créer une demande d'explication");
    }

    private String generateNumeroDemande() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = demandeRepository.count() + 1;
        return String.format("EXP-%s-%04d", year, count);
    }
}