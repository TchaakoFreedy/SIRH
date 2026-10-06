package com.fric.sirh.service;

import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.ConfigurationConge;
import com.fric.sirh.model.Employee;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ConfigurationCongeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ConfigurationCongeServiceImpl implements ConfigurationCongeService {

    private final ConfigurationCongeRepository repository;
    private final EmployeeService employeeService;
    private final NotificationPublisher notificationPublisher;

    public ConfigurationCongeServiceImpl(ConfigurationCongeRepository repository,
                                         EmployeeService employeeService,
                                         NotificationPublisher notificationPublisher) {
        this.repository = repository;
        this.employeeService = employeeService;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public ConfigurationConge create(ConfigurationConge config) {
        log.info("Création d'une configuration : {}", config);

        if (config.getType() == null || config.getType().isEmpty()) {
            throw new IllegalArgumentException("Le type de configuration est obligatoire");
        }

        // GLOBALE : une seule sans année
        if ("GLOBALE".equals(config.getType())) {
            Optional<ConfigurationConge> existing = repository.findByTypeAndAnneeIsNull("GLOBALE");
            if (existing.isPresent()) {
                throw new IllegalStateException("Une configuration globale existe déjà. Utilisez update.");
            }
        }

        // GENRE : une seule par genre sans année
        if ("GENRE".equals(config.getType()) && config.getGenre() != null) {
            Optional<ConfigurationConge> existing = repository.findByTypeAndGenreAndAnneeIsNull("GENRE", config.getGenre());
            if (existing.isPresent()) {
                throw new IllegalStateException("Une configuration pour ce genre existe déjà. Utilisez update.");
            }
        }

        // INDIVIDUELLE : une seule par employé sans année
        if ("INDIVIDUELLE".equals(config.getType()) && config.getEmployeeId() != null) {
            Optional<ConfigurationConge> existing = repository.findByTypeAndEmployeeId("INDIVIDUELLE", config.getEmployeeId());
            if (existing.isPresent()) {
                throw new IllegalStateException("Une configuration individuelle existe déjà pour cet employé. Utilisez update.");
            }
        }

        // Si bonusEnfantActif est null, le mettre à false
        if (config.getBonusEnfantActif() == null) {
            config.setBonusEnfantActif(false);
        }

        config.setCreatedAt(LocalDate.now());
        config.setUpdatedAt(LocalDate.now());
        log.info("✅ Configuration créée avec succès : type={}, employeeId={}, joursDeBase={}",
                config.getType(), config.getEmployeeId(), config.getJoursDeBase());

        ConfigurationConge saved = repository.save(config);

        // Publication de la notification
        publishNotificationForConfig(saved, "create");

        return saved;
    }

    @Override
    public ConfigurationConge update(String id, ConfigurationConge config) {
        log.info("Mise à jour de la configuration {} : {}", id, config);
        ConfigurationConge existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConfigurationConge", id));

        existing.setNom(config.getNom());
        existing.setJoursDeBase(config.getJoursDeBase());
        existing.setBonusEnfantActif(config.getBonusEnfantActif());
        existing.setJoursParEnfant(config.getJoursParEnfant());
        existing.setAgeMaxEnfant(config.getAgeMaxEnfant());
        existing.setAnnee(config.getAnnee());
        existing.setUpdatedAt(LocalDate.now());
        existing.setUpdatedBy(config.getUpdatedBy());

        log.info("✅ Configuration mise à jour : type={}, employeeId={}, joursDeBase={}",
                existing.getType(), existing.getEmployeeId(), existing.getJoursDeBase());

        ConfigurationConge updated = repository.save(existing);

        // Publication de la notification
        publishNotificationForConfig(updated, "update");

        return updated;
    }

    @Override
    public void delete(String id) {
        log.info("Suppression de la configuration {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("ConfigurationConge", id);
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<ConfigurationConge> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<ConfigurationConge> getAll() {
        return repository.findAll();
    }

    @Override
    public ConfigurationConge getGlobalConfiguration() {
        return repository.findByTypeAndAnneeIsNull("GLOBALE")
                .orElseThrow(() -> new ResourceNotFoundException("Configuration globale non trouvée"));
    }

    /**
     * ✅ Récupère la configuration pour un employé (lecture seule)
     * Ordre de priorité : Individuelle > Genre > Globale > Défaut
     */
    @Override
    public ConfigurationConge getConfigurationForEmployee(String employeeId) {
        log.info("🔍 Recherche configuration pour employé: {}", employeeId);

        // 1. Vérifier si une configuration individuelle existe
        Optional<ConfigurationConge> individual = repository.findByTypeAndEmployeeId("INDIVIDUELLE", employeeId);
        if (individual.isPresent()) {
            log.info("✅ Configuration INDIVIDUELLE trouvée pour: {}", employeeId);
            return individual.get();
        }

        // 2. Vérifier si une configuration par genre existe
        try {
            Employee employee = employeeService.getById(employeeId);
            String genre = employee.getSexe();
            if (genre != null) {
                String normalized = normalizeGenre(genre);
                Optional<ConfigurationConge> genreConfig = repository.findByTypeAndGenreAndAnneeIsNull("GENRE", normalized);
                if (genreConfig.isPresent()) {
                    log.info("✅ Configuration GENRE trouvée pour: {}", normalized);
                    return genreConfig.get();
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer le genre de l'employé {}", employeeId, e);
        }

        // 3. Vérifier si une configuration globale existe
        Optional<ConfigurationConge> global = repository.findByTypeAndAnneeIsNull("GLOBALE");
        if (global.isPresent()) {
            log.info("✅ Configuration GLOBALE trouvée");
            return global.get();
        }

        // 4. ✅ AUCUNE CONFIGURATION TROUVÉE - CRÉER UNE CONFIGURATION PAR DÉFAUT
        log.warn("⚠️ Aucune configuration trouvée pour l'employé {}, création d'une configuration par défaut", employeeId);
        return createDefaultConfiguration();
    }

    /**
     * ✅ Récupère ou crée une configuration individuelle pour un employé (pour modification)
     */
    @Override
    public ConfigurationConge getOrCreateIndividualConfiguration(String employeeId) {
        log.info("🔍 Recherche ou création d'une configuration individuelle pour: {}", employeeId);

        // 1. Vérifier si une configuration individuelle existe déjà
        Optional<ConfigurationConge> existing = repository.findByTypeAndEmployeeId("INDIVIDUELLE", employeeId);
        if (existing.isPresent()) {
            log.info("✅ Configuration INDIVIDUELLE existante trouvée pour: {}", employeeId);
            return existing.get();
        }

        // 2. Récupérer les valeurs par défaut (genre ou globale)
        ConfigurationConge defaultConfig = getConfigurationForEmployee(employeeId);

        // 3. Créer une configuration individuelle basée sur les valeurs par défaut
        ConfigurationConge individual = new ConfigurationConge();
        individual.setType("INDIVIDUELLE");
        individual.setEmployeeId(employeeId);
        individual.setNom("Configuration individuelle");
        individual.setJoursDeBase(defaultConfig.getJoursDeBase());

        // ✅ Correction: Utiliser Boolean.TRUE.equals() pour éviter les problèmes de types
        Boolean bonusActif = defaultConfig.getBonusEnfantActif();
        individual.setBonusEnfantActif(bonusActif != null && bonusActif);

        // ✅ Correction: Utiliser des vérifications null-safe
        Integer joursParEnfant = defaultConfig.getJoursParEnfant();
        individual.setJoursParEnfant(joursParEnfant != null ? joursParEnfant : 0);

        Integer ageMax = defaultConfig.getAgeMaxEnfant();
        individual.setAgeMaxEnfant(ageMax != null ? ageMax : 0);

        individual.setAnnee(null);
        individual.setCreatedAt(LocalDate.now());
        individual.setUpdatedAt(LocalDate.now());
        individual.setUpdatedBy("SYSTEM");

        ConfigurationConge saved = repository.save(individual);
        log.info("✅ Configuration INDIVIDUELLE créée pour: {} avec {} jours", employeeId, saved.getJoursDeBase());

        // Publier une notification
        try {
            publishNotificationForConfig(saved, "create_individual");
        } catch (Exception e) {
            log.warn("⚠️ Impossible de publier la notification pour la configuration individuelle", e);
        }

        return saved;
    }

    @Override
    public ConfigurationConge getConfigurationForEmployeeAndYear(String employeeId, Integer annee) {
        log.info("🔍 Recherche configuration pour employé: {} et année: {}", employeeId, annee);

        // 1. Individuelle avec année
        Optional<ConfigurationConge> individual = repository.findByTypeAndEmployeeIdAndAnnee("INDIVIDUELLE", employeeId, annee);
        if (individual.isPresent()) {
            log.info("✅ Configuration INDIVIDUELLE avec année trouvée pour: {}", employeeId);
            return individual.get();
        }

        // 2. Individuelle sans année
        individual = repository.findByTypeAndEmployeeId("INDIVIDUELLE", employeeId);
        if (individual.isPresent()) {
            log.info("✅ Configuration INDIVIDUELLE sans année trouvée pour: {}", employeeId);
            return individual.get();
        }

        // 3. Genre avec année
        try {
            Employee employee = employeeService.getById(employeeId);
            String genre = employee.getSexe();
            if (genre != null) {
                String normalized = normalizeGenre(genre);
                Optional<ConfigurationConge> genreConfig = repository.findByTypeAndGenreAndAnnee("GENRE", normalized, annee);
                if (genreConfig.isPresent()) {
                    log.info("✅ Configuration GENRE avec année trouvée pour: {}", normalized);
                    return genreConfig.get();
                }
                genreConfig = repository.findByTypeAndGenreAndAnneeIsNull("GENRE", normalized);
                if (genreConfig.isPresent()) {
                    log.info("✅ Configuration GENRE sans année trouvée pour: {}", normalized);
                    return genreConfig.get();
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer le genre de l'employé {}", employeeId, e);
        }

        // 4. Globale avec année
        Optional<ConfigurationConge> global = repository.findByTypeAndAnnee("GLOBALE", annee);
        if (global.isPresent()) {
            log.info("✅ Configuration GLOBALE avec année trouvée");
            return global.get();
        }

        // 5. Globale sans année
        Optional<ConfigurationConge> globalWithoutYear = repository.findByTypeAndAnneeIsNull("GLOBALE");
        if (globalWithoutYear.isPresent()) {
            log.info("✅ Configuration GLOBALE sans année trouvée");
            return globalWithoutYear.get();
        }

        // 6. ✅ AUCUNE CONFIGURATION TROUVÉE - CRÉER UNE CONFIGURATION PAR DÉFAUT
        log.warn("⚠️ Aucune configuration trouvée pour l'employé {} et l'année {}, création d'une configuration par défaut",
                employeeId, annee);
        return createDefaultConfiguration();
    }

    /**
     * ✅ Crée une configuration par défaut
     */
    private ConfigurationConge createDefaultConfiguration() {
        log.info("📝 Création d'une configuration par défaut");

        // Vérifier si une configuration globale existe déjà
        Optional<ConfigurationConge> existingGlobal = repository.findByTypeAndAnneeIsNull("GLOBALE");
        if (existingGlobal.isPresent()) {
            log.info("✅ Utilisation de la configuration globale existante");
            return existingGlobal.get();
        }

        // Créer une configuration globale par défaut
        ConfigurationConge defaultConfig = new ConfigurationConge();
        defaultConfig.setType("GLOBALE");
        defaultConfig.setNom("Configuration par défaut");
        defaultConfig.setJoursDeBase(30);
        defaultConfig.setBonusEnfantActif(false);
        defaultConfig.setJoursParEnfant(0);
        defaultConfig.setAgeMaxEnfant(0);
        defaultConfig.setAnnee(null);
        defaultConfig.setCreatedAt(LocalDate.now());
        defaultConfig.setUpdatedAt(LocalDate.now());
        defaultConfig.setUpdatedBy("SYSTEM");

        ConfigurationConge saved = repository.save(defaultConfig);
        log.info("✅ Configuration par défaut créée avec ID: {}", saved.getId());

        // Publier une notification (optionnel)
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("entityId", saved.getId());
            data.put("entityType", "CONFIGURATION_CONGE");
            data.put("actionUrl", "/configurations-conge/" + saved.getId());
            data.put("metadata", Map.of(
                    "type", saved.getType(),
                    "joursDeBase", saved.getJoursDeBase(),
                    "action", "create_default"
            ));
            notificationPublisher.publish(
                    NotificationEvent.LEAVE_BALANCE_GLOBAL_UPDATED,
                    data,
                    "SYSTEM"
            );
        } catch (Exception e) {
            log.warn("⚠️ Impossible de publier la notification pour la configuration par défaut", e);
        }

        return saved;
    }

    private String normalizeGenre(String genre) {
        if (genre == null) return null;
        if (genre.equalsIgnoreCase("F") || genre.equalsIgnoreCase("FEMME")) return "FEMME";
        if (genre.equalsIgnoreCase("M") || genre.equalsIgnoreCase("HOMME")) return "HOMME";
        return genre.toUpperCase();
    }

    /**
     * Publie une notification pour une configuration
     */
    private void publishNotificationForConfig(ConfigurationConge config, String action) {
        try {
            NotificationEvent event;
            Map<String, Object> data = new HashMap<>();
            String triggeredBy = config.getUpdatedBy() != null ? config.getUpdatedBy() : "SYSTEM";

            if ("GLOBALE".equals(config.getType())) {
                event = NotificationEvent.LEAVE_BALANCE_GLOBAL_UPDATED;
                data.put("entityId", config.getId());
                data.put("entityType", "CONFIGURATION_CONGE");
                data.put("actionUrl", "/configurations-conge/" + config.getId());
                data.put("metadata", Map.of(
                        "type", config.getType(),
                        "joursDeBase", config.getJoursDeBase(),
                        "action", action
                ));
            } else {
                event = NotificationEvent.LEAVE_BALANCE_INDIVIDUAL_UPDATED;
                data.put("employeeId", config.getEmployeeId());
                data.put("companyId", getCompanyIdForEmployee(config.getEmployeeId()));
                data.put("departmentId", getDepartmentIdForEmployee(config.getEmployeeId()));
                data.put("entityId", config.getId());
                data.put("entityType", "CONFIGURATION_CONGE");
                data.put("actionUrl", "/configurations-conge/" + config.getId());
                data.put("metadata", Map.of(
                        "type", config.getType(),
                        "joursDeBase", config.getJoursDeBase(),
                        "action", action
                ));
            }

            notificationPublisher.publish(event, data, triggeredBy);
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la publication de la notification: {}", e.getMessage());
        }
    }

    private String getCompanyIdForEmployee(String employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);
            return employee.getEntrepriseId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getDepartmentIdForEmployee(String employeeId) {
        try {
            Employee employee = employeeService.getById(employeeId);
            return employee.getDepartementId();
        } catch (Exception e) {
            return null;
        }
    }
}