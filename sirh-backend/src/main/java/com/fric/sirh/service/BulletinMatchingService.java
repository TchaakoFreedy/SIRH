package com.fric.sirh.service;

import com.fric.sirh.model.Employee;
import com.fric.sirh.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BulletinMatchingService {

    private final EmployeeRepository employeeRepository;

    public Employee matchEmployee(Map<String, String> extractedData) {
        String matricule = extractedData.get("matricule");
        String nom = extractedData.get("nom");
        String netAPayer = extractedData.get("netAPayer");

        log.info("🔍 Recherche employé - Matricule: '{}', Nom: '{}', Net à payer: '{}'", matricule, nom, netAPayer);

        // 1. Recherche EXCLUSIVE par Matricule
        if (matricule != null && !matricule.isEmpty()) {
            String cleanMatricule = matricule.trim().replaceAll("\\s+", "");
            log.info("📌 Recherche par MATRICULE: '{}'", cleanMatricule);

            Employee emp = employeeRepository.findByMatriculeInterne(cleanMatricule).orElse(null);
            if (emp != null) {
                log.info("✅ Employé trouvé par matricule: {} - {} {}",
                        emp.getMatriculeInterne(), emp.getPrenom(), emp.getNom());
                return emp;
            }

            List<Employee> candidates = employeeRepository.searchEmployees(cleanMatricule);
            if (!candidates.isEmpty()) {
                log.info("✅ Employé trouvé via searchEmployees pour matricule '{}': {} - {} {}",
                        cleanMatricule,
                        candidates.get(0).getMatriculeInterne(),
                        candidates.get(0).getPrenom(),
                        candidates.get(0).getNom());
                return candidates.get(0);
            }

            log.warn("⚠️ Aucun employé trouvé par MATRICULE '{}'", cleanMatricule);
        }

        // 2. Fallback par Nom (si pas de matricule)
        if (nom != null && !nom.isEmpty() && !nom.equalsIgnoreCase("null") && !nom.equalsIgnoreCase("Nom")) {
            String cleanNom = nom.replaceAll("\\s+", " ").trim();
            log.info("📌 Recherche par NOM (fallback): '{}'", cleanNom);

            List<Employee> byName = employeeRepository.findByNomContainingIgnoreCase(cleanNom);
            if (!byName.isEmpty()) {
                log.info("✅ Employé trouvé par nom: {} - {} {}",
                        byName.get(0).getMatriculeInterne(),
                        byName.get(0).getPrenom(),
                        byName.get(0).getNom());
                return byName.get(0);
            }
        }

        log.warn("❌ Aucun employé trouvé pour les données: {}", extractedData);
        return null;
    }
}