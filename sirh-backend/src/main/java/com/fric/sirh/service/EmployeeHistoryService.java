package com.fric.sirh.service;

import com.fric.sirh.dto.history.EmployeeHistoryEvent;
import com.fric.sirh.dto.history.EmployeeHistoryResponse;
import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;
import com.fric.sirh.model.*;
import com.fric.sirh.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeHistoryService {

    private final EmployeeRepository employeeRepository;
    private final ContratRepository contratRepository;
    private final CongeRepository congeRepository;
    private final AbsenceRepository absenceRepository;
    private final DocumentsRepository documentsRepository;
    private final BulletinPaieRepository bulletinPaieRepository;
    private final PerformanceRepository performanceRepository;
    private final SoldeCongeRepository soldeCongeRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;

    public EmployeeHistoryResponse getEmployeeHistory(String employeeId) {
        // ... (identique à la version précédente, inchangé)
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employe non trouve avec l'id : " + employeeId));

        List<EmployeeHistoryEvent> events = new ArrayList<>();

        // 1. Embauche
        if (employee.getDate_embauche() != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("Matricule", employee.getMatriculeInterne());
            String posteLibelle = getPosteLibelle(employee.getPosteId());
            details.put("Poste", posteLibelle != null ? posteLibelle : "Non défini");
            String deptNom = getDepartementNom(employee.getDepartementId());
            details.put("Département", deptNom != null ? deptNom : "Non défini");
            String entrepriseNom = getEntrepriseNom(employee.getEntrepriseId());
            details.put("Entreprise", entrepriseNom != null ? entrepriseNom : "Non définie");
            events.add(EmployeeHistoryEvent.builder()
                    .date(employee.getDate_embauche().atStartOfDay())
                    .type("EMBAUCHE")
                    .description("Embauche de l'employé")
                    .details(details)
                    .build());
        }

        // 2. Statut
        if (employee.getUpdatedAt() != null && employee.getStatut() != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            String statutFr = traduireStatut(employee.getStatut());
            details.put("Statut", statutFr);
            events.add(EmployeeHistoryEvent.builder()
                    .date(employee.getUpdatedAt().atStartOfDay())
                    .type("STATUS_CHANGE")
                    .description("Statut actuel : " + statutFr)
                    .details(details)
                    .build());
        }

        // 3. Contrats
        List<Contrat> contrats = contratRepository.findByEmployee_Id(employeeId);
        for (Contrat contrat : contrats) {
            if (contrat.getDateDebut() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                String typeContratFr = traduireTypeContrat(contrat.getTypeContrat());
                details.put("Type de contrat", typeContratFr);
                details.put("Statut", traduireStatutContrat(contrat.getStatut()));
                if (contrat.getSalaireBrut() != null) {
                    details.put("Salaire brut", String.format("%.2f FCFA", contrat.getSalaireBrut()));
                }
                events.add(EmployeeHistoryEvent.builder()
                        .date(contrat.getDateDebut().atStartOfDay())
                        .type("DEBUT_DU_CONTRACT")
                        .description("Début du contrat " + typeContratFr)
                        .details(details)
                        .build());
            }
            if (contrat.getDateFin() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                String typeContratFr = traduireTypeContrat(contrat.getTypeContrat());
                details.put("Type de contrat", typeContratFr);
                details.put("Motif", "Fin de contrat");
                events.add(EmployeeHistoryEvent.builder()
                        .date(contrat.getDateFin().atStartOfDay())
                        .type("FIN_DU_CONTRACT")
                        .description("Fin du contrat " + typeContratFr)
                        .details(details)
                        .build());
            }
            if (contrat.getDateResiliation() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                String typeContratFr = traduireTypeContrat(contrat.getTypeContrat());
                details.put("Type de contrat", typeContratFr);
                details.put("Motif", contrat.getMotifResiliation());
                events.add(EmployeeHistoryEvent.builder()
                        .date(contrat.getDateResiliation().atStartOfDay())
                        .type("TERMINATION DU CONTRACT")
                        .description("Résiliation du contrat " + typeContratFr)
                        .details(details)
                        .build());
            }
        }

        // 4. Congés
        List<Conge> conges = congeRepository.findByEmployee(employee);
        for (Conge conge : conges) {
            if (conge.getJourDebut() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                String typeCongeFr = traduireTypeConge(conge.getTypeConge());
                details.put("Type de congé", typeCongeFr);
                details.put("Nombre de jours", conge.getNbJour());
                details.put("Statut", traduireStatutConge(conge.getStatut()));
                events.add(EmployeeHistoryEvent.builder()
                        .date(conge.getJourDebut().atStartOfDay())
                        .type("CONGES")
                        .description("Congé " + typeCongeFr + " (" + conge.getNbJour() + " jours)")
                        .details(details)
                        .build());
            }
        }

        // 5. Absences
        List<Absence> absences = absenceRepository.findByEmployeeId(employeeId);
        for (Absence absence : absences) {
            if (absence.getDateDebut() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("Motif", absence.getMotif());
                details.put("Statut", traduireStatutAbsence(absence.getStatut()));
                events.add(EmployeeHistoryEvent.builder()
                        .date(absence.getDateDebut().atStartOfDay())
                        .type("ABSENCE")
                        .description("Absence : " + absence.getMotif())
                        .details(details)
                        .build());
            }
        }

        // 6. Documents
        List<Documents> documents = documentsRepository.findByEmployee_Id(employeeId);
        for (Documents doc : documents) {
            if (doc.getDateUpload() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                String typeDocFr = traduireTypeDocument(doc.getTypeDocument());
                details.put("Type", typeDocFr);
                details.put("Nom", doc.getName());
                events.add(EmployeeHistoryEvent.builder()
                        .date(doc.getDateUpload().atStartOfDay())
                        .type("DOCUMENT")
                        .description("Document " + typeDocFr + " : " + doc.getName())
                        .details(details)
                        .build());
            }
        }

        // 7. Bulletins de paie
        List<BulletinPaie> bulletins = bulletinPaieRepository.findByEmployeeId(employeeId);
        for (BulletinPaie bulletin : bulletins) {
            if (bulletin.getCreatedAt() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("Période", bulletin.getPeriod());
                if (bulletin.getNetSalary() != null) {
                    details.put("Salaire net", String.format("%.2f FCFA", bulletin.getNetSalary()));
                }
                if (bulletin.getGrossSalary() != null) {
                    details.put("Salaire brut", String.format("%.2f FCFA", bulletin.getGrossSalary()));
                }
                events.add(EmployeeHistoryEvent.builder()
                        .date(bulletin.getCreatedAt())
                        .type("BULLETIN DE PAIE")
                        .description("Bulletin de paie " + bulletin.getPeriod())
                        .details(details)
                        .build());
            }
        }

        // 8. Performances
        List<Performance> performances = performanceRepository.findByEmployeeId(employeeId);
        for (Performance perf : performances) {
            if (perf.getCreatedAt() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("Période", perf.getPeriode());
                details.put("Note", perf.getNote());
                details.put("Description", perf.getDescription());
                events.add(EmployeeHistoryEvent.builder()
                        .date(perf.getCreatedAt().atStartOfDay())
                        .type("EVALUATION DES PERFORMANCES")
                        .description("Évaluation de performance : " + perf.getPeriode())
                        .details(details)
                        .build());
            }
        }

        // 9. Soldes de congés
        List<SoldeConge> soldes = soldeCongeRepository.findByEmployeeId(employeeId);
        for (SoldeConge solde : soldes) {
            if (solde.getUpdatedAt() != null) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("Année", solde.getAnnee());
                details.put("Jours total", solde.getJourTotal());
                details.put("Jours utilisés", solde.getJourUtiliser());
                details.put("Jours restants", solde.getJourRestant());
                events.add(EmployeeHistoryEvent.builder()
                        .date(solde.getUpdatedAt().atStartOfDay())
                        .type("SOLDE DES CONGES")
                        .description("Mise à jour du solde de congés pour " + solde.getAnnee())
                        .details(details)
                        .build());
            }
        }

        // Tri par date croissante
        events.sort(Comparator.comparing(EmployeeHistoryEvent::getDate, Comparator.nullsLast(Comparator.naturalOrder())));

        return EmployeeHistoryResponse.builder()
                .employeeId(employeeId)
                .employeeName(employee.getPrenom() + " " + employee.getNom())
                .events(events)
                .build();
    }

    // ===== Méthodes d'enrichissement =====

    private String getPosteLibelle(String posteId) {
        if (posteId == null) return null;
        return posteRepository.findById(posteId)
                .map(Poste::getLibelle)
                .orElse(null);
    }

    private String getDepartementNom(String departementId) {
        if (departementId == null) return null;
        return departementRepository.findById(departementId)
                .map(Departement::getName)
                .orElse(null);
    }

    private String getEntrepriseNom(String entrepriseId) {
        if (entrepriseId == null) return null;
        return entrepriseRepository.findById(entrepriseId)
                .map(Entreprise::getName)
                .orElse(null);
    }

    private String traduireStatut(String statut) {
        if (statut == null) return "Non défini";
        switch (statut.toUpperCase()) {
            case "ACTIF": return "Actif";
            case "SUSPENDU": return "Suspendu";
            case "INACTIF": return "Inactif";
            case "RESILIE": return "Résilié";
            default: return statut;
        }
    }

    private String traduireTypeContrat(String type) {
        if (type == null) return "Non défini";
        switch (type.toUpperCase()) {
            case "CDI": return "CDI";
            case "CDD": return "CDD";
            case "ESSAI": return "Contrat d'essai";
            case "STAGE_ACADEMIQUE": return "Stage académique";
            case "STAGE_PROFESSIONNEL": return "Stage professionnel";
            case "FREELANCE": return "Freelance";
            default: return type;
        }
    }

    private String traduireStatutContrat(String statut) {
        if (statut == null) return "Non défini";
        switch (statut.toUpperCase()) {
            case "ACTIF": return "Actif";
            case "EN_ATTENTE": return "En attente";
            case "SUSPENDU": return "Suspendu";
            case "EXPIRE": return "Expiré";
            case "RESILIE": return "Résilié";
            case "ARCHIVE": return "Archivé";
            case "EN_ESSAI": return "Période d'essai";
            default: return statut;
        }
    }

    private String traduireTypeConge(TypeConge type) {
        if (type == null) return "Non défini";
        switch (type) {
            case ANNUEL: return "Annuel";
            case PERMISSION: return "Permission";
            case ABSENCE: return "Absence";
            default: return type.name();
        }
    }

    private String traduireStatutConge(StatutConge statut) {
        if (statut == null) return "Non défini";
        switch (statut) {
            case EN_ATTENTE: return "En attente";
            case APPROUVE: return "Approuvé";
            case REJETE: return "Rejeté";
            case ANNULE: return "Annulé";
            default: return statut.name();
        }
    }

    private String traduireStatutAbsence(String statut) {
        if (statut == null) return "Non défini";
        switch (statut.toUpperCase()) {
            case "JUSTIFIEE": return "Justifiée";
            case "NON_JUSTIFIEE": return "Non justifiée";
            case "EN_ATTENTE": return "En attente";
            default: return statut;
        }
    }

    private String traduireTypeDocument(String type) {
        if (type == null) return "Autre";
        switch (type.toUpperCase()) {
            case "CNI": return "CNI / Passeport";
            case "CERTIFICAT": return "Certificat";
            case "PHOTO": return "Photo d'identité";
            case "DIPLOME": return "Diplôme";
            case "CONTRAT_SIGNE": return "Contrat signé";
            case "BULLETIN_PAIE": return "Bulletin de paie";
            case "CV": return "CV";
            case "PASSEPORT": return "Passeport";
            default: return type;
        }
    }

    // ===== GÉNÉRATION CSV AMÉLIORÉE =====

    public byte[] generateCsv(EmployeeHistoryResponse history) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // En-tête
        writer.println("Date;Type;Description;Détails");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (EmployeeHistoryEvent event : history.getEvents()) {
            String dateStr = event.getDate() != null ? event.getDate().format(formatter) : "";
            String type = event.getType() != null ? event.getType() : "";
            String desc = event.getDescription() != null ? event.getDescription() : "";

            // Construction des détails sans accolades ni "="
            StringBuilder detailsBuilder = new StringBuilder();
            if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (Map.Entry<String, Object> entry : event.getDetails().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        parts.add(key + " : " + value.toString());
                    }
                }
                detailsBuilder.append(String.join(" | ", parts));
            }

            String details = detailsBuilder.toString();

            // Écrire la ligne (séparateur ; pour éviter les conflits avec les virgules)
            writer.printf("%s;%s;%s;%s%n", dateStr, type, desc, details);
        }

        writer.close();
        return baos.toByteArray();
    }
}