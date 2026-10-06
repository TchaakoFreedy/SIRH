package com.fric.sirh.service;

import com.fric.sirh.dto.PaiementRequest;
import com.fric.sirh.dto.SoldeEmployeResponse;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Paiement;
import com.fric.sirh.model.TypePaiement;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.PaiementRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final EmployeeRepository employeeRepository;
    private final ContratRepository contratRepository;
    private final DepartementRepository departementRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==========================================
    // SOLDE D'UN EMPLOYE
    // ==========================================

    public SoldeEmployeResponse getSoldeEmploye(
            String employeeId,
            Integer mois,
            Integer annee
    ) {
        validateMoisAnnee(mois, annee);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException(
                        "Employe introuvable avec l'id : " + employeeId
                ));

        return computeSolde(employee, mois, annee);
    }

    public List<SoldeEmployeResponse> getSoldesAllEmployees(
            Integer mois,
            Integer annee
    ) {
        validateMoisAnnee(mois, annee);

        List<Employee> employees = employeeRepository.findAll();

        List<SoldeEmployeResponse> soldes = new ArrayList<>();

        for (Employee employee : employees) {
            try {
                soldes.add(computeSolde(employee, mois, annee));
            } catch (Exception e) {
                log.error(
                        "Erreur calcul solde employe {} : {}",
                        employee.getId(),
                        e.getMessage()
                );
            }
        }

        return soldes;
    }

    private SoldeEmployeResponse computeSolde(
            Employee employee,
            Integer mois,
            Integer annee
    ) {
        Double salaire = resolveSalaireMensuel(employee);

        List<Paiement> paiementsMois =
                paiementRepository.findByEmployeeIdAndMoisAndAnnee(
                        employee.getId(),
                        mois,
                        annee
                );

        double totalAvances = paiementsMois.stream()
                .filter(p -> p.getType() == TypePaiement.AVANCE)
                .mapToDouble(p -> p.getMontant() != null ? p.getMontant() : 0d)
                .sum();

        double totalRetenues = paiementsMois.stream()
                .filter(p -> p.getType() == TypePaiement.RETENUE)
                .mapToDouble(p -> p.getMontant() != null ? p.getMontant() : 0d)
                .sum();

        double totalPaye = paiementsMois.stream()
                .filter(p -> p.getType() == TypePaiement.PAIEMENT_SALAIRE)
                .mapToDouble(p -> p.getMontant() != null ? p.getMontant() : 0d)
                .sum();

        double salaireBase = salaire != null ? salaire : 0d;
        double netAPayer = salaireBase - totalAvances - totalRetenues;
        if (netAPayer < 0) {
            netAPayer = 0d;
        }

        double restant = netAPayer - totalPaye;
        if (restant < 0) {
            restant = 0d;
        }

        SoldeEmployeResponse response = new SoldeEmployeResponse();
        response.setEmployeeId(employee.getId());
        response.setEmployeeNom(employee.getNom());
        response.setEmployeePrenom(employee.getPrenom());
        response.setEmployeeMatricule(employee.getMatriculeInterne());
        response.setEmployeePoste(employee.getPosteId());
        response.setEmployeeDepartement(resolveDepartementName(employee.getDepartementId()));
        response.setEmployeeTelephone(employee.getTelephone());

        response.setSalaireMensuel(salaireBase);
        response.setMois(mois);
        response.setAnnee(annee);

        response.setTotalAvances(totalAvances);
        response.setTotalRetenues(totalRetenues);
        response.setTotalPaye(totalPaye);

        response.setMontantNetAPayer(netAPayer);
        response.setMontantRestant(restant);

        response.setSalaireConfigure(salaire != null && salaire > 0);
        response.setSoldeDisponible(restant > 0);

        return response;
    }

    private Double resolveSalaireMensuel(Employee employee) {

        if (employee.getSalaireMensuel() != null
                && employee.getSalaireMensuel() > 0) {
            return employee.getSalaireMensuel();
        }

        try {
            List<Contrat> contrats =
                    contratRepository.findByEmployee_Id(employee.getId());

            Optional<Contrat> actif = contrats.stream()
                    .filter(c -> "ACTIF".equalsIgnoreCase(c.getStatut()))
                    .findFirst();

            if (actif.isPresent()) {
                Contrat contrat = actif.get();
                if (contrat.getSalaireNet() != null
                        && contrat.getSalaireNet() > 0) {
                    return contrat.getSalaireNet();
                }
                if (contrat.getSalaireBrut() != null
                        && contrat.getSalaireBrut() > 0) {
                    return contrat.getSalaireBrut();
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Impossible de recuperer le contrat pour l'employe {} : {}",
                    employee.getId(),
                    e.getMessage()
            );
        }

        return null;
    }

    private String resolveDepartementName(String departementId) {
        if (departementId == null || departementId.trim().isEmpty()) {
            return "";
        }
        try {
            return departementRepository.findById(departementId)
                    .map(Departement::getName)
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    // ==========================================
    // ENREGISTREMENT D'UN PAIEMENT / AVANCE / RETENUE
    // ==========================================

    @Transactional
    public Paiement enregistrerPaiement(
            PaiementRequest request,
            String creator
    ) {
        validateRequest(request);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employe introuvable avec l'id : "
                                + request.getEmployeeId()
                ));

        SoldeEmployeResponse solde =
                computeSolde(employee, request.getMois(), request.getAnnee());

        if (!solde.isSalaireConfigure()) {
            throw new IllegalStateException(
                    "Le salaire mensuel de cet employe n'est pas configure."
            );
        }

        if (request.getType() == TypePaiement.AVANCE
                || request.getType() == TypePaiement.PAIEMENT_SALAIRE) {

            if (request.getMontant() > solde.getMontantRestant() + 0.001) {
                throw new IllegalStateException(
                        "Le montant saisi (" + request.getMontant()
                                + ") depasse le solde restant ("
                                + solde.getMontantRestant() + ")."
                );
            }
        }

        if (request.getType() == TypePaiement.RETENUE) {
            if (request.getMotif() == null
                    || request.getMotif().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Le motif est obligatoire pour une retenue."
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Paiement paiement = new Paiement();
        paiement.setEmployeeId(employee.getId());
        paiement.setEmployeeNom(employee.getNom());
        paiement.setEmployeePrenom(employee.getPrenom());
        paiement.setEmployeeMatricule(employee.getMatriculeInterne());
        paiement.setEmployeePoste(employee.getPosteId());
        paiement.setEmployeeDepartement(
                resolveDepartementName(employee.getDepartementId())
        );
        paiement.setEmployeeTelephone(employee.getTelephone());
        paiement.setEmployeeEmail(
                employee.getUser() != null ? employee.getUser().getEmail() : null
        );
        paiement.setEmployeeDateEmbauche(employee.getDate_embauche());

        paiement.setType(request.getType());
        paiement.setMontant(request.getMontant());
        paiement.setMotif(
                request.getMotif() != null
                        ? request.getMotif().trim()
                        : null
        );
        paiement.setMois(request.getMois());
        paiement.setAnnee(request.getAnnee());

        paiement.setDatePaiement(now);
        paiement.setNumeroRecu(generateNumeroRecu(now));

        double salaireBase = solde.getSalaireMensuel();
        double avancesApres = solde.getTotalAvances();
        double retenuesApres = solde.getTotalRetenues();
        double payeApres = solde.getTotalPaye();

        if (request.getType() == TypePaiement.AVANCE) {
            avancesApres += request.getMontant();
        } else if (request.getType() == TypePaiement.RETENUE) {
            retenuesApres += request.getMontant();
        } else if (request.getType() == TypePaiement.PAIEMENT_SALAIRE) {
            payeApres += request.getMontant();
        }

        double netApres = salaireBase - avancesApres - retenuesApres;
        if (netApres < 0) {
            netApres = 0;
        }
        double restantApres = netApres - payeApres;
        if (restantApres < 0) {
            restantApres = 0;
        }

        paiement.setSalaireMensuel(salaireBase);
        paiement.setTotalAvancesMois(avancesApres);
        paiement.setTotalRetenuesMois(retenuesApres);
        paiement.setTotalPayeMois(payeApres);
        paiement.setMontantNetAPayer(netApres);
        paiement.setMontantRestantApres(restantApres);

        paiement.setCreatedBy(creator);
        paiement.setCreatedAt(now);

        Paiement saved = paiementRepository.save(paiement);

        log.info(
                "Paiement enregistre : {} - {} - {} FCFA (mois {}/{})",
                saved.getId(),
                saved.getType(),
                saved.getMontant(),
                saved.getMois(),
                saved.getAnnee()
        );

        return saved;
    }

    private String generateNumeroRecu(LocalDateTime date) {
        String datePart = date.format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        String randomPart = String.format(
                "%04d",
                new Random().nextInt(10000)
        );
        return "RECU-" + datePart + "-" + randomPart;
    }

    // ==========================================
    // HISTORIQUE
    // ==========================================

    public List<Paiement> getAllPaiements() {
        return paiementRepository.findAllByOrderByDatePaiementDesc();
    }

    public List<Paiement> getPaiementsByEmployee(String employeeId) {
        return paiementRepository.findByEmployeeIdOrderByDatePaiementDesc(employeeId);
    }

    public List<Paiement> getPaiementsByMoisAnnee(Integer mois, Integer annee) {
        return paiementRepository.findByMoisAndAnnee(mois, annee);
    }

    // ==========================================
    // SUPPRESSION
    // ==========================================

    @Transactional
    public void deletePaiement(String id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Paiement introuvable : " + id
                ));
        paiementRepository.delete(paiement);
        log.info("Paiement {} supprime", id);
    }

    // ==========================================
    // RECU PDF
    // ==========================================

    public byte[] generateRecuPdf(String paiementId) {

        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException(
                        "Paiement introuvable : " + paiementId
                ));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    18,
                    new Color(1, 147, 147)
            );

            Font subTitleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    12,
                    new Color(74, 104, 103)
            );

            Font labelFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    10,
                    new Color(74, 104, 103)
            );

            Font valueFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10,
                    Color.BLACK
            );

            Paragraph header = new Paragraph("FRIC SIRH", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph(
                    "Recu de paiement",
                    subTitleFont
            );
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(new Paragraph(" "));

            Paragraph recuInfo = new Paragraph(
                    "Numero de recu : " + safe(paiement.getNumeroRecu())
                            + "\nDate : "
                            + (paiement.getDatePaiement() != null
                            ? paiement.getDatePaiement().format(DATE_TIME_FORMATTER)
                            : "-"),
                    valueFont
            );
            document.add(recuInfo);

            document.add(new Paragraph(" "));

            PdfPTable employeeTable = new PdfPTable(2);
            employeeTable.setWidthPercentage(100);
            employeeTable.setWidths(new float[]{1.2f, 2.8f});

            addTableRow(employeeTable, "Nom", safe(paiement.getEmployeeNom()), labelFont, valueFont);
            addTableRow(employeeTable, "Prenom", safe(paiement.getEmployeePrenom()), labelFont, valueFont);
            addTableRow(employeeTable, "Matricule", safe(paiement.getEmployeeMatricule()), labelFont, valueFont);
            addTableRow(employeeTable, "Poste", safe(paiement.getEmployeePoste()), labelFont, valueFont);
            addTableRow(employeeTable, "Departement", safe(paiement.getEmployeeDepartement()), labelFont, valueFont);
            addTableRow(employeeTable, "Telephone", safe(paiement.getEmployeeTelephone()), labelFont, valueFont);
            addTableRow(employeeTable, "Email", safe(paiement.getEmployeeEmail()), labelFont, valueFont);
            addTableRow(employeeTable, "Date embauche",
                    paiement.getEmployeeDateEmbauche() != null
                            ? paiement.getEmployeeDateEmbauche().toString()
                            : "-",
                    labelFont, valueFont);

            document.add(employeeTable);

            document.add(new Paragraph(" "));

            Paragraph periode = new Paragraph(
                    "Periode : " + safe(String.valueOf(paiement.getMois()))
                            + "/" + safe(String.valueOf(paiement.getAnnee())),
                    subTitleFont
            );
            document.add(periode);

            document.add(new Paragraph(" "));

            PdfPTable salaireTable = new PdfPTable(2);
            salaireTable.setWidthPercentage(100);
            salaireTable.setWidths(new float[]{2f, 2f});

            addTableRow(salaireTable, "Salaire mensuel",
                    formatMoney(paiement.getSalaireMensuel()), labelFont, valueFont);
            addTableRow(salaireTable, "Total avances",
                    formatMoney(paiement.getTotalAvancesMois()), labelFont, valueFont);
            addTableRow(salaireTable, "Total retenues",
                    formatMoney(paiement.getTotalRetenuesMois()), labelFont, valueFont);
            addTableRow(salaireTable, "Montant net a payer",
                    formatMoney(paiement.getMontantNetAPayer()), labelFont, valueFont);
            addTableRow(salaireTable, "Total deja paye",
                    formatMoney(paiement.getTotalPayeMois()), labelFont, valueFont);
            addTableRow(salaireTable, "Montant restant apres",
                    formatMoney(paiement.getMontantRestantApres()), labelFont, valueFont);

            document.add(salaireTable);

            document.add(new Paragraph(" "));

            PdfPTable actionTable = new PdfPTable(2);
            actionTable.setWidthPercentage(100);
            actionTable.setWidths(new float[]{1.2f, 2.8f});

            addTableRow(actionTable, "Type",
                    paiement.getType() != null ? paiement.getType().name() : "-",
                    labelFont, valueFont);
            addTableRow(actionTable, "Montant paye",
                    formatMoney(paiement.getMontant()), labelFont, valueFont);
            addTableRow(actionTable, "Motif",
                    safe(paiement.getMotif()), labelFont, valueFont);

            document.add(actionTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Paragraph signature = new Paragraph(
                    "Signature de l'employe : ______________________________",
                    valueFont
            );
            signature.setAlignment(Element.ALIGN_RIGHT);
            document.add(signature);

            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph(
                    "Ce recu a ete genere automatiquement par le systeme FRIC SIRH.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY)
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            log.error("Erreur generation PDF : {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Impossible de generer le recu PDF : " + e.getMessage(),
                    e
            );
        }

        return baos.toByteArray();
    }

    private void addTableRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(PdfPCell.NO_BORDER);
        cellLabel.setPadding(6);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, valueFont));
        cellValue.setBorder(PdfPCell.NO_BORDER);
        cellValue.setPadding(6);

        table.addCell(cellLabel);
        table.addCell(cellValue);
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }

    private String formatMoney(Double value) {
        if (value == null) {
            return "0 FCFA";
        }
        return String.format("%,.0f FCFA", value).replace(',', ' ');
    }

    // ==========================================
    // SALAIRE MENSUEL
    // ==========================================

    @Transactional
    public Employee setSalaireMensuel(
            String employeeId,
            Double salaireMensuel,
            String updater
    ) {
        if (salaireMensuel == null || salaireMensuel < 0) {
            throw new IllegalArgumentException(
                    "Le salaire mensuel doit etre un nombre positif."
            );
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException(
                        "Employe introuvable : " + employeeId
                ));

        employee.setSalaireMensuel(salaireMensuel);
        employee.setUpdatedAt(LocalDate.now());
        employee.setUpdatedBy(updater);

        return employeeRepository.save(employee);
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    private void validateRequest(PaiementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requete est obligatoire.");
        }
        if (request.getEmployeeId() == null
                || request.getEmployeeId().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "L'identifiant de l'employe est obligatoire."
            );
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException(
                    "Le type de paiement est obligatoire."
            );
        }
        if (request.getMontant() == null || request.getMontant() <= 0) {
            throw new IllegalArgumentException(
                    "Le montant doit etre superieur a 0."
            );
        }
        validateMoisAnnee(request.getMois(), request.getAnnee());
    }

    private void validateMoisAnnee(Integer mois, Integer annee) {
        if (mois == null || mois < 1 || mois > 12) {
            throw new IllegalArgumentException(
                    "Le mois doit etre compris entre 1 et 12."
            );
        }
        if (annee == null || annee < 2000 || annee > 2100) {
            throw new IllegalArgumentException(
                    "L'annee est invalide."
            );
        }
    }
}