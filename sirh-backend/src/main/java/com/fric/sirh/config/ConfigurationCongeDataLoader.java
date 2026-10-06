package com.fric.sirh.config;

import com.fric.sirh.model.ConfigurationConge;
import com.fric.sirh.repository.ConfigurationCongeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationCongeDataLoader implements CommandLineRunner {

    private final ConfigurationCongeRepository repository;

    @Override
    public void run(String... args) {
        log.info("🔧 Vérification des configurations de congés...");

        try {
            // 1. Configuration GLOBALE
            boolean globalExists = repository.findByTypeAndAnneeIsNull("GLOBALE").isPresent();
            if (!globalExists) {
                log.info("📝 Création de la configuration GLOBALE par défaut...");
                ConfigurationConge global = new ConfigurationConge();
                global.setType("GLOBALE");
                global.setNom("Configuration globale par défaut");
                global.setJoursDeBase(24);
                global.setBonusEnfantActif(false);
                global.setJoursParEnfant(0);
                global.setAgeMaxEnfant(0);
                global.setAnnee(null);
                global.setCreatedAt(LocalDate.now());
                global.setUpdatedAt(LocalDate.now());
                global.setUpdatedBy("SYSTEM");
                repository.save(global);
                log.info("✅ Configuration GLOBALE créée avec succès !");
            } else {
                log.info("ℹ️ Configuration GLOBALE existe déjà");
            }


            log.info("✅ Initialisation des configurations de congés terminée !");

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation des configurations de congés", e);
        }
    }
}