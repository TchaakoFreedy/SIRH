package com.fric.sirh.seeder;

import com.fric.sirh.performance.model.CriterePerformance;
import com.fric.sirh.performance.repository.CriterePerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class CriterePerformanceSeeder implements CommandLineRunner {

    private final CriterePerformanceRepository critereRepository;

    @Override
    public void run(String... args) {
        // Vérifier si des critères existent déjà
        if (critereRepository.count() > 0) {
            log.info("Des critères de performance existent déjà, skipping seeder");
            return;
        }

        List<CriterePerformance> criteres = Arrays.asList(
                createCritere("Ponctualité", "Respect des horaires de travail", 10, 2),
                createCritere("Discipline", "Respect des règles et procédures", 10, 2),
                createCritere("Travail en équipe", "Capacité à collaborer avec les autres", 10, 2),
                createCritere("Leadership", "Capacité à diriger et motiver", 10, 2),
                createCritere("Productivité", "Quantité de travail réalisée", 10, 2),
                createCritere("Qualité du travail", "Qualité des réalisations", 10, 2),
                createCritere("Communication", "Capacité à communiquer efficacement", 10, 2),
                createCritere("Initiative", "Proactivité et prise d'initiative", 10, 2),
                createCritere("Respect des procédures", "Respect des processus établis", 10, 2),
                createCritere("Objectifs atteints", "Atteinte des objectifs fixés", 10, 3)
        );

        critereRepository.saveAll(criteres);
        log.info("✅ {} critères de performance créés", criteres.size());
    }

    private CriterePerformance createCritere(String nom, String description, Integer noteMaximale, Integer coefficient) {
        return CriterePerformance.builder()
                .nom(nom)
                .description(description)
                .noteMaximale(noteMaximale)
                .coefficient(coefficient)
                .actif(true)
                .createdBy("SYSTEM")
                .createdAt(LocalDateTime.now())
                .build();
    }
}