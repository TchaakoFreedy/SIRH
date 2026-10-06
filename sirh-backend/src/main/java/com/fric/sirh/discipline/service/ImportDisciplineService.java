// com.fric.sirh.discipline.service.ImportDisciplineService.java

package com.fric.sirh.discipline.service;

import com.fric.sirh.discipline.dto.ImportDemandeExplicationDTO;
import com.fric.sirh.discipline.dto.ImportHistoriqueDTO;
import com.fric.sirh.discipline.dto.ImportReponseDTO;
import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import com.fric.sirh.discipline.enums.TypeActionHistorique;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.model.HistoriqueDiscipline;
import com.fric.sirh.discipline.model.ReponseExplication;
import com.fric.sirh.discipline.repository.DemandeExplicationRepository;
import com.fric.sirh.discipline.repository.ReponseExplicationRepository;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportDisciplineService {

    private final DemandeExplicationRepository demandeRepository;
    private final ReponseExplicationRepository reponseRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ImportResult importerDemandeExplication(ImportDemandeExplicationDTO dto) {
        log.info("🔍 Importation d'une demande d'explication: {}", dto.getNumeroOriginal());

        try {
            // Vérifier si la demande existe déjà
            if (demandeRepository.existsByNumero(dto.getNumeroOriginal())) {
                log.warn("⚠️ La demande {} existe déjà", dto.getNumeroOriginal());
                return ImportResult.duplicate(dto.getNumeroOriginal());
            }

            // 1. Trouver l'employé concerné
            Employee employeConcerne = findEmployee(dto.getEmployeConcerneIdentifier());
            if (employeConcerne == null) {
                log.error("❌ Employé non trouvé: {}", dto.getEmployeConcerneIdentifier());
                return ImportResult.failure(dto.getNumeroOriginal(),
                        "Employé non trouvé: " + dto.getEmployeConcerneIdentifier());
            }

            if (employeConcerne.getId() == null || employeConcerne.getId().isEmpty()) {
                log.error("❌ L'employé trouvé n'a pas d'ID valide");
                return ImportResult.failure(dto.getNumeroOriginal(),
                        "L'employé trouvé n'a pas d'ID valide");
            }

            // Recharger l'employé
            Optional<Employee> employeeReloaded = employeeRepository.findById(employeConcerne.getId());
            if (employeeReloaded.isEmpty()) {
                log.error("❌ Impossible de recharger l'employé avec l'ID: {}", employeConcerne.getId());
                return ImportResult.failure(dto.getNumeroOriginal(),
                        "Impossible de recharger l'employé: " + employeConcerne.getId());
            }
            employeConcerne = employeeReloaded.get();

            log.info("✅ Employé rechargé: {} - ID: {}",
                    employeConcerne.getNom() + " " + (employeConcerne.getPrenom() != null ? employeConcerne.getPrenom() : ""),
                    employeConcerne.getId());

            // 2. Trouver l'auteur
            User auteur = findUser(dto.getAuteurIdentifier());
            if (auteur == null) {
                auteur = findDefaultRHUser();
                if (auteur == null) {
                    log.error("❌ Aucun utilisateur RH par défaut trouvé");
                    return ImportResult.failure(dto.getNumeroOriginal(),
                            "Auteur non trouvé et aucun RH par défaut disponible");
                }
                log.warn("⚠️ Auteur non trouvé, utilisation de l'utilisateur RH par défaut: {}",
                        auteur.getEmail());
            }

            if (auteur.getId() == null || auteur.getId().isEmpty()) {
                log.error("❌ L'auteur trouvé n'a pas d'ID valide");
                return ImportResult.failure(dto.getNumeroOriginal(),
                        "L'auteur trouvé n'a pas d'ID valide");
            }

            // Recharger l'utilisateur
            Optional<User> userReloaded = userRepository.findById(auteur.getId());
            if (userReloaded.isEmpty()) {
                log.error("❌ Impossible de recharger l'utilisateur avec l'ID: {}", auteur.getId());
                return ImportResult.failure(dto.getNumeroOriginal(),
                        "Impossible de recharger l'utilisateur: " + auteur.getId());
            }
            auteur = userReloaded.get();

            log.info("✅ Auteur rechargé: {} - ID: {}",
                    auteur.getFullName(),
                    auteur.getId());

            // 3. Construire et sauvegarder la demande SANS la réponse
            DemandeExplication demande = new DemandeExplication();

            demande.setNumero(dto.getNumeroOriginal());
            demande.setObjet(dto.getObjet());
            demande.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
            demande.setMotif(dto.getMotif() != null ? dto.getMotif() : "");

            demande.setEmployeConcerne(employeConcerne);
            demande.setAuteur(auteur);

            demande.setEntrepriseId(employeConcerne.getEntrepriseId());
            demande.setDepartementId(employeConcerne.getDepartementId());

            demande.setDateCreation(dto.getDateCreation() != null ? dto.getDateCreation() : LocalDateTime.now());
            demande.setDateLimiteReponse(dto.getDateLimiteReponse() != null ?
                    dto.getDateLimiteReponse() : LocalDateTime.now().plusDays(15));

            // Statut initial
            demande.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutDemandeExplication.EN_ATTENTE);

            demande.setCreatedBy(auteur.getId());
            demande.setCreatedAt(demande.getDateCreation());
            demande.setUpdatedBy(auteur.getId());
            demande.setUpdatedAt(LocalDateTime.now());

            // 4. Ajouter l'historique
            if (dto.getHistorique() != null && !dto.getHistorique().isEmpty()) {
                for (ImportHistoriqueDTO histoDto : dto.getHistorique()) {
                    HistoriqueDiscipline historique = new HistoriqueDiscipline();
                    historique.setUtilisateurId(auteur.getId());
                    historique.setUtilisateurNom(auteur.getFullName());
                    historique.setAction(histoDto.getAction() != null ? histoDto.getAction() : TypeActionHistorique.DEMANDE_CREEE);
                    historique.setDate(histoDto.getDate() != null ? histoDto.getDate() : LocalDateTime.now());
                    historique.setCommentaire(histoDto.getCommentaire() != null ?
                            histoDto.getCommentaire() : "Import historique");
                    demande.getHistorique().add(historique);
                }
            } else {
                HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                        .utilisateurId(auteur.getId())
                        .utilisateurNom(auteur.getFullName())
                        .action(TypeActionHistorique.DEMANDE_CREEE)
                        .date(demande.getDateCreation())
                        .commentaire("Demande importée depuis l'historique")
                        .build();
                demande.getHistorique().add(historique);
            }

            // 5. SAUVEGARDER D'ABORD LA DEMANDE pour générer son ID
            DemandeExplication savedDemande = demandeRepository.save(demande);
            log.info("✅ Demande sauvegardée avec ID: {}", savedDemande.getId());

            // 6. Gérer la réponse APRÈS la sauvegarde de la demande
            if (dto.getReponse() != null) {
                ReponseExplication reponse = new ReponseExplication();
                // Utiliser la demande sauvegardée avec son ID
                reponse.setDemandeExplication(savedDemande);
                reponse.setContenu(dto.getReponse().getContenu() != null ? dto.getReponse().getContenu() : "");
                reponse.setPiecesJointes(dto.getReponse().getPiecesJointes());
                reponse.setDateReponse(dto.getReponse().getDateReponse() != null ?
                        dto.getReponse().getDateReponse() : LocalDateTime.now());
                reponse.setCreatedBy(auteur.getId());
                reponse.setCreatedAt(reponse.getDateReponse());

                ReponseExplication savedReponse = reponseRepository.save(reponse);
                log.info("✅ Réponse sauvegardée avec ID: {}", savedReponse.getId());

                // Mettre à jour la demande avec la réponse
                savedDemande.setReponse(savedReponse);
                savedDemande.setStatut(determinerStatutFinal(dto));
                savedDemande.setUpdatedBy(auteur.getId());
                savedDemande.setUpdatedAt(LocalDateTime.now());

                // Re-sauvegarder la demande avec la réponse
                savedDemande = demandeRepository.save(savedDemande);
                log.info("✅ Demande mise à jour avec la réponse");
            }

            return ImportResult.success(dto.getNumeroOriginal(), savedDemande.getId());

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'importation de {}: {}", dto.getNumeroOriginal(), e.getMessage(), e);
            return ImportResult.failure(dto.getNumeroOriginal(), e.getMessage());
        }
    }

    @Transactional
    public List<ImportResult> importerDemandesEnMasse(List<ImportDemandeExplicationDTO> dtos) {
        log.info("🔍 Import en masse de {} demandes", dtos.size());

        List<ImportResult> results = new ArrayList<>();

        for (ImportDemandeExplicationDTO dto : dtos) {
            ImportResult result = importerDemandeExplication(dto);
            results.add(result);
        }

        long successCount = results.stream().filter(ImportResult::isSuccess).count();
        long failureCount = results.stream().filter(ImportResult::isFailure).count();
        long duplicateCount = results.stream().filter(ImportResult::isDuplicate).count();

        log.info("📊 Import terminé: {} succès, {} échecs, {} doublons",
                successCount, failureCount, duplicateCount);

        return results;
    }

    private StatutDemandeExplication determinerStatutFinal(ImportDemandeExplicationDTO dto) {
        if (dto.getReponse() != null) {
            if (dto.getReponse().isValidee()) {
                return StatutDemandeExplication.VALIDEE;
            } else if (dto.getReponse().isRejetee()) {
                return StatutDemandeExplication.REJETEE;
            }
            return StatutDemandeExplication.REPONDUE;
        }
        return dto.getStatut() != null ? dto.getStatut() : StatutDemandeExplication.EN_ATTENTE;
    }

    private Employee findEmployee(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        log.debug("🔍 Recherche de l'employé avec l'identifiant: {}", identifier);

        try {
            Optional<Employee> byId = employeeRepository.findById(identifier);
            if (byId.isPresent()) {
                Employee emp = byId.get();
                if (emp.getId() != null && !emp.getId().isEmpty()) {
                    log.debug("✅ Employé trouvé par ID: {}", emp.getId());
                    return emp;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par ID: {}", e.getMessage());
        }

        try {
            Optional<Employee> byUserId = employeeRepository.findByUserId(identifier);
            if (byUserId.isPresent()) {
                Employee emp = byUserId.get();
                if (emp.getId() != null && !emp.getId().isEmpty()) {
                    log.debug("✅ Employé trouvé par userId: {}", emp.getId());
                    return emp;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par userId: {}", e.getMessage());
        }

        try {
            Optional<Employee> byUserEmail = employeeRepository.findByUserEmail(identifier);
            if (byUserEmail.isPresent()) {
                Employee emp = byUserEmail.get();
                if (emp.getId() != null && !emp.getId().isEmpty()) {
                    log.debug("✅ Employé trouvé par email de l'utilisateur: {}", emp.getId());
                    return emp;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par email de l'utilisateur: {}", e.getMessage());
        }

        try {
            Optional<Employee> byMatricule = employeeRepository.findByMatriculeInterne(identifier);
            if (byMatricule.isPresent()) {
                Employee emp = byMatricule.get();
                if (emp.getId() != null && !emp.getId().isEmpty()) {
                    log.debug("✅ Employé trouvé par matricule: {}", emp.getId());
                    return emp;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par matricule: {}", e.getMessage());
        }

        try {
            String[] parts = identifier.trim().split(" ");
            if (parts.length >= 2) {
                String prenom = parts[0];
                String nom = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

                List<Employee> byPrenom = employeeRepository.findByPrenomContainingIgnoreCase(prenom);
                for (Employee emp : byPrenom) {
                    if (emp.getNom() != null && emp.getNom().equalsIgnoreCase(nom)) {
                        if (emp.getId() != null && !emp.getId().isEmpty()) {
                            log.debug("✅ Employé trouvé par nom complet: {}", emp.getId());
                            return emp;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par nom complet: {}", e.getMessage());
        }

        log.warn("⚠️ Aucun employé trouvé pour l'identifiant: {}", identifier);
        return null;
    }

    private User findUser(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;

        log.debug("🔍 Recherche de l'utilisateur avec l'identifiant: {}", identifier);

        try {
            Optional<User> byId = userRepository.findById(identifier);
            if (byId.isPresent()) {
                User user = byId.get();
                if (user.getId() != null && !user.getId().isEmpty()) {
                    log.debug("✅ Utilisateur trouvé par ID: {}", user.getId());
                    return user;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par ID: {}", e.getMessage());
        }

        try {
            Optional<User> byEmail = userRepository.findByEmail(identifier);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                if (user.getId() != null && !user.getId().isEmpty()) {
                    log.debug("✅ Utilisateur trouvé par email: {}", user.getId());
                    return user;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par email: {}", e.getMessage());
        }

        try {
            Optional<User> byEmployeeId = userRepository.findByEmployeeId(identifier);
            if (byEmployeeId.isPresent()) {
                User user = byEmployeeId.get();
                if (user.getId() != null && !user.getId().isEmpty()) {
                    log.debug("✅ Utilisateur trouvé par employeeId: {}", user.getId());
                    return user;
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par employeeId: {}", e.getMessage());
        }

        try {
            String[] parts = identifier.trim().split(" ");
            if (parts.length >= 2) {
                String firstName = parts[0];
                String lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

                List<User> byFirstName = userRepository.findByFirstNameContainingIgnoreCase(firstName);
                for (User user : byFirstName) {
                    if (user.getLastName() != null && user.getLastName().equalsIgnoreCase(lastName)) {
                        if (user.getId() != null && !user.getId().isEmpty()) {
                            log.debug("✅ Utilisateur trouvé par nom complet: {}", user.getId());
                            return user;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la recherche par nom complet: {}", e.getMessage());
        }

        log.warn("⚠️ Aucun utilisateur trouvé pour l'identifiant: {}", identifier);
        return null;
    }

    private User findDefaultRHUser() {
        try {
            List<User> activeUsers = userRepository.findByActiveTrue();
            if (!activeUsers.isEmpty()) {
                User user = activeUsers.get(0);
                if (user.getId() != null && !user.getId().isEmpty()) {
                    log.info("✅ Utilisateur RH par défaut trouvé: {}", user.getEmail());
                    return user;
                }
            }

            List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                User user = allUsers.get(0);
                if (user.getId() != null && !user.getId().isEmpty()) {
                    log.info("✅ Utilisateur par défaut trouvé: {}", user.getEmail());
                    return user;
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche du RH par défaut: {}", e.getMessage());
        }
        return null;
    }

    // ==================== CLASSES DE RÉSULTAT ====================

    public static class ImportResult {
        private final String numeroOriginal;
        private final String demandeId;
        private final boolean success;
        private final boolean duplicate;
        private final String errorMessage;

        private ImportResult(String numeroOriginal, String demandeId,
                             boolean success, boolean duplicate, String errorMessage) {
            this.numeroOriginal = numeroOriginal;
            this.demandeId = demandeId;
            this.success = success;
            this.duplicate = duplicate;
            this.errorMessage = errorMessage;
        }

        public static ImportResult success(String numero, String id) {
            return new ImportResult(numero, id, true, false, null);
        }

        public static ImportResult failure(String numero, String error) {
            return new ImportResult(numero, null, false, false, error);
        }

        public static ImportResult duplicate(String numero) {
            return new ImportResult(numero, null, false, true, "Demande déjà existante");
        }

        public String getNumeroOriginal() { return numeroOriginal; }
        public String getDemandeId() { return demandeId; }
        public boolean isSuccess() { return success; }
        public boolean isDuplicate() { return duplicate; }
        public boolean isFailure() { return !success && !duplicate; }
        public String getErrorMessage() { return errorMessage; }
    }
}