package com.fric.sirh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fric.sirh.dto.ContratDTO;
import com.fric.sirh.dto.CreateContratRequest;
import com.fric.sirh.dto.RenouvellementContratRequest;
import com.fric.sirh.dto.StatistiquesContratDTO;
import com.fric.sirh.dto.UpdateContratRequest;
import com.fric.sirh.exception.ContractConflictException;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Employee;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratService {

    private final ContratRepository contratRepository;
    private final EmployeeRepository employeeRepository;
    private final ImageStorageService imageStorageService;
    private final NotificationPublisher notificationPublisher;

    // ==========================================
    // ENTITY -> DTO (surcharge avec Employee explicite)
    // ==========================================
    private ContratDTO toDTO(Contrat contrat, Employee employee) {
        ContratDTO dto = new ContratDTO();
        dto.setId(contrat.getId());
        dto.setTypeContrat(contrat.getTypeContrat());
        dto.setDateDebut(contrat.getDateDebut());
        dto.setDateFin(contrat.getDateFin());
        dto.setStatut(contrat.getStatut());
        dto.setImageUrls(contrat.getImageUrls() == null ? new ArrayList<>() : contrat.getImageUrls());
        dto.setCreatedAt(contrat.getCreatedAt());
        dto.setCreatedBy(contrat.getCreatedBy());
        dto.setUpdatedAt(contrat.getUpdatedAt());
        dto.setUpdatedBy(contrat.getUpdatedBy());

        // Salaires
        dto.setSalaireBrut(contrat.getSalaireBrut());
        dto.setSalaireNet(contrat.getSalaireNet());
        dto.setTauxHoraire(contrat.getTauxHoraire());
        dto.setNombreHeuresSemaine(contrat.getNombreHeuresSemaine());

        // Période d'essai
        dto.setDateFinEssai(contrat.getDateFinEssai());
        dto.setDureeEssaiMois(contrat.getDureeEssaiMois());

        // CDD
        dto.setMotifRecours(contrat.getMotifRecours());
        dto.setDureeMois(contrat.getDureeMois());

        // Stage
        dto.setEtablissement(contrat.getEtablissement());
        dto.setTuteurNom(contrat.getTuteurNom());
        dto.setTuteurEmail(contrat.getTuteurEmail());
        dto.setTuteurTelephone(contrat.getTuteurTelephone());
        dto.setObjectifsStage(contrat.getObjectifsStage());
        dto.setDureeSemaines(contrat.getDureeSemaines());

        // Freelance
        dto.setDescriptionPrestation(contrat.getDescriptionPrestation());
        dto.setModalitesPaiement(contrat.getModalitesPaiement());
        dto.setDureeMoisPrestation(contrat.getDureeMoisPrestation());

        // Renouvellement
        dto.setEstRenouvelable(contrat.getEstRenouvelable());
        dto.setNombreRenouvellements(contrat.getNombreRenouvellements());
        dto.setRenouvellementMax(contrat.getRenouvellementMax());
        dto.setEstRenouvele(contrat.getEstRenouvele());
        dto.setContratPrecedentId(contrat.getContratPrecedentId());

        // Résiliation
        dto.setMotifResiliation(contrat.getMotifResiliation());
        dto.setDateResiliation(contrat.getDateResiliation());

        dto.setObservations(contrat.getObservations());

        // Employé : priorité à l'employé passé en paramètre, sinon celui du contrat
        Employee emp = employee != null ? employee : contrat.getEmployee();
        if (emp != null) {
            dto.setEmployeeId(emp.getId());
            dto.setEmployeeNom(emp.getNom());
            dto.setEmployeePrenom(emp.getPrenom());
            dto.setEmployeeMatricule(emp.getMatriculeInterne());
        }

        return dto;
    }

    // Méthode de base utilisant l'employé du contrat
    private ContratDTO toDTO(Contrat contrat) {
        return toDTO(contrat, contrat.getEmployee());
    }

    // ==========================================
    // CREATE CONTRAT WITH IMAGES (CORRIGE)
    // ==========================================
    @Transactional
    public ContratDTO creerContrat(CreateContratRequest request, MultipartFile[] files, String userId) {
        log.info("Creation contrat pour employe: {}, replaceActive={}", request.getEmployeeId(), request.getReplaceActive());

        Employee employee = employeeRepository
                .findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employe non trouve"));

        // ---- VERIFICATION DES CONTRATS ACTIFS (utilisation de la méthode fiable) ----
        List<Contrat> actifs = contratRepository.findByEmployee_IdAndStatut(employee.getId(), "ACTIF");
        log.info("Nombre de contrats actifs trouves pour l'employe {}: {}", employee.getId(), actifs.size());

        if (!actifs.isEmpty()) {
            if (Boolean.TRUE.equals(request.getReplaceActive())) {
                // Desactiver tous les contrats actifs de cet employe
                log.info("Desactivation de {} contrat(s) actif(s) pour l'employe {}", actifs.size(), employee.getId());
                for (Contrat actif : actifs) {
                    actif.setStatut("RESILIE");
                    actif.setDateResiliation(LocalDate.now());
                    actif.setMotifResiliation("Remplace par nouveau contrat");
                    actif.setUpdatedAt(LocalDate.now());
                    actif.setUpdatedBy(userId);
                    contratRepository.save(actif);
                    log.debug("Contrat {} desactive", actif.getId());
                }
            } else {
                throw new ContractConflictException("L'employe a deja un contrat actif. Veuillez confirmer le remplacement.");
            }
        }

        // ---- CREATION DU NOUVEAU CONTRAT ----
        Contrat contrat = new Contrat();
        contrat.setEmployee(employee);
        contrat.setTypeContrat(request.getTypeContrat());
        contrat.setDateDebut(request.getDateDebut());
        contrat.setDateFin(request.getDateFin());
        contrat.setStatut(request.getStatut() != null ? request.getStatut() : "ACTIF");
        contrat.setImageUrls(new ArrayList<>());
        contrat.setCreatedAt(LocalDate.now());
        contrat.setCreatedBy(userId);

        // Mappage des champs optionnels avec conversion BigDecimal -> Double
        contrat.setSalaireBrut(request.getSalaireBrut() != null ? request.getSalaireBrut().doubleValue() : null);
        contrat.setSalaireNet(request.getSalaireNet() != null ? request.getSalaireNet().doubleValue() : null);
        contrat.setTauxHoraire(request.getTauxHoraire() != null ? request.getTauxHoraire().doubleValue() : null);
        contrat.setNombreHeuresSemaine(request.getNombreHeuresSemaine());
        contrat.setDateFinEssai(request.getDateFinEssai());
        contrat.setDureeEssaiMois(request.getDureeEssaiMois());
        contrat.setMotifRecours(request.getMotifRecours());
        contrat.setDureeMois(request.getDureeMois());
        contrat.setEtablissement(request.getEtablissement());
        contrat.setTuteurNom(request.getTuteurNom());
        contrat.setTuteurEmail(request.getTuteurEmail());
        contrat.setTuteurTelephone(request.getTuteurTelephone());
        contrat.setObjectifsStage(request.getObjectifsStage());
        contrat.setDureeSemaines(request.getDureeSemaines());
        contrat.setDescriptionPrestation(request.getDescriptionPrestation());
        contrat.setModalitesPaiement(request.getModalitesPaiement());
        contrat.setDureeMoisPrestation(request.getDureeMoisPrestation());
        contrat.setEstRenouvelable(request.getEstRenouvelable() != null ? request.getEstRenouvelable() : false);
        contrat.setRenouvellementMax(request.getRenouvellementMax());
        contrat.setObservations(request.getObservations());

        contrat = contratRepository.save(contrat);
        log.info("Contrat cree avec ID: {}", contrat.getId());

        // ---- TRAITEMENT DES FICHIERS ----
        if (files != null && files.length > 0) {
            log.info("Upload de {} fichiers pour le contrat {}", files.length, contrat.getId());
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String imageUrl = imageStorageService.storeImage(file, "contrats", contrat.getId());
                        imageUrls.add(imageUrl);
                        log.info("Image uploadée: {}", imageUrl);
                    } catch (IOException e) {
                        log.error("Erreur lors de l'upload de l'image", e);
                        throw new RuntimeException("Erreur lors de l'upload de l'image: " + e.getMessage());
                    }
                }
            }

            if (!imageUrls.isEmpty()) {
                contrat.setImageUrls(imageUrls);
                contrat.setUpdatedAt(LocalDate.now());
                contrat.setUpdatedBy(userId);
                contrat = contratRepository.save(contrat);
                log.info("{} images ajoutees au contrat", imageUrls.size());
            }
        }

        publishContractEvent(contrat, "CREATE", userId, employee);

        return toDTO(contrat, employee);
    }

    // ==========================================
    // UPLOAD IMAGES FOR EXISTING CONTRACT
    // ==========================================
    @Transactional
    public ContratDTO uploadContratImages(String contratId, MultipartFile[] files, String userId) {
        log.info("Upload d'images pour le contrat: {}", contratId);

        Contrat contrat = getEntity(contratId);
        Employee employee = contrat.getEmployee();

        List<String> imageUrls = contrat.getImageUrls();
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String imageUrl = imageStorageService.storeImage(file, "contrats", contratId);
                        imageUrls.add(imageUrl);
                        log.info("Image uploadée: {}", imageUrl);
                    } catch (IOException e) {
                        log.error("Erreur lors de l'upload de l'image", e);
                        throw new RuntimeException("Erreur lors de l'upload de l'image: " + e.getMessage());
                    }
                }
            }
        }

        contrat.setImageUrls(imageUrls);
        contrat.setUpdatedAt(LocalDate.now());
        contrat.setUpdatedBy(userId);

        Contrat saved = contratRepository.save(contrat);
        log.info("{} images uploadées pour le contrat {}", imageUrls.size(), contratId);

        publishDocumentUploadEvent(saved, userId);

        return toDTO(saved, employee);
    }

    // ==========================================
    // GET CONTRACT IMAGES
    // ==========================================
    public List<String> getContratImages(String contratId) {
        Contrat contrat = getEntity(contratId);
        return contrat.getImageUrls() != null ? contrat.getImageUrls() : new ArrayList<>();
    }

    // ==========================================
    // DOWNLOAD CONTRACT IMAGE
    // ==========================================
    public ResponseEntity<byte[]> downloadContratImage(String contratId, int imageIndex) {
        Contrat contrat = getEntity(contratId);
        List<String> imageUrls = contrat.getImageUrls();

        if (imageUrls == null || imageUrls.isEmpty() || imageIndex >= imageUrls.size()) {
            throw new RuntimeException("Image non trouvee");
        }

        String imageUrl = imageUrls.get(imageIndex);
        try {
            byte[] imageData = imageStorageService.downloadImage(imageUrl);
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, getContentType(extension))
                    .body(imageData);
        } catch (IOException e) {
            log.error("Erreur lors du telechargement de l'image", e);
            throw new RuntimeException("Erreur lors du telechargement de l'image: " + e.getMessage());
        }
    }

    // ==========================================
    // VIEW CONTRACT IMAGE
    // ==========================================
    public ResponseEntity<byte[]> viewContratImage(String contratId, int imageIndex) {
        Contrat contrat = getEntity(contratId);
        List<String> imageUrls = contrat.getImageUrls();

        if (imageUrls == null || imageUrls.isEmpty() || imageIndex >= imageUrls.size()) {
            throw new RuntimeException("Image non trouvee");
        }

        String imageUrl = imageUrls.get(imageIndex);
        try {
            byte[] imageData = imageStorageService.downloadImage(imageUrl);
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, getContentType(extension))
                    .body(imageData);
        } catch (IOException e) {
            log.error("Erreur lors de la visualisation de l'image", e);
            throw new RuntimeException("Erreur lors de la visualisation de l'image: " + e.getMessage());
        }
    }

    // ==========================================
    // DELETE CONTRACT IMAGE
    // ==========================================
    @Transactional
    public ContratDTO deleteContratImage(String contratId, int imageIndex, String userId) {
        Contrat contrat = getEntity(contratId);
        Employee employee = contrat.getEmployee();

        List<String> imageUrls = contrat.getImageUrls();

        if (imageUrls == null || imageUrls.isEmpty() || imageIndex >= imageUrls.size()) {
            throw new RuntimeException("Image non trouvee");
        }

        String imageUrl = imageUrls.get(imageIndex);
        try {
            imageStorageService.deleteImage(imageUrl);
            imageUrls.remove(imageIndex);
            contrat.setImageUrls(imageUrls);
            contrat.setUpdatedAt(LocalDate.now());
            contrat.setUpdatedBy(userId);

            Contrat saved = contratRepository.save(contrat);
            log.info("Image supprimee du contrat {}", contratId);

            return toDTO(saved, employee);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de l'image", e);
            throw new RuntimeException("Erreur lors de la suppression de l'image: " + e.getMessage());
        }
    }

    // ==========================================
    // UPDATE (modifierContrat) - CORRIGE AVEC LOGS ET VALIDATION RENFORCEE
    // ==========================================
    @Transactional
    public ContratDTO modifierContrat(String id, UpdateContratRequest request, String userId) {
        log.info("Demande de modification du contrat ID: {}", id);

        // Charger le contrat existant
        Contrat contrat = getEntity(id);
        log.info("Contrat charge avec ID: {}, Statut: {}, DateFin: {}", contrat.getId(), contrat.getStatut(), contrat.getDateFin());

        Employee employee = contrat.getEmployee();

        // Sauvegarde des anciennes valeurs pour les logs
        String ancienStatut = contrat.getStatut();
        LocalDate ancienneDateFin = contrat.getDateFin();

        // Mappage de tous les champs
        if (request.getTypeContrat() != null) {
            contrat.setTypeContrat(request.getTypeContrat());
        }
        if (request.getDateDebut() != null) {
            contrat.setDateDebut(request.getDateDebut());
        }
        if (request.getDateFin() != null) {
            contrat.setDateFin(request.getDateFin());
        }
        if (request.getStatut() != null) {
            contrat.setStatut(request.getStatut());
        }
        // Les salaires sont déjà en Double dans UpdateContratRequest
        if (request.getSalaireBrut() != null) {
            contrat.setSalaireBrut(request.getSalaireBrut());
        }
        if (request.getSalaireNet() != null) {
            contrat.setSalaireNet(request.getSalaireNet());
        }
        if (request.getTauxHoraire() != null) {
            contrat.setTauxHoraire(request.getTauxHoraire());
        }
        if (request.getNombreHeuresSemaine() != null) {
            contrat.setNombreHeuresSemaine(request.getNombreHeuresSemaine());
        }
        if (request.getDateFinEssai() != null) {
            contrat.setDateFinEssai(request.getDateFinEssai());
        }
        if (request.getDureeEssaiMois() != null) {
            contrat.setDureeEssaiMois(request.getDureeEssaiMois());
        }
        if (request.getMotifRecours() != null) {
            contrat.setMotifRecours(request.getMotifRecours());
        }
        if (request.getDureeMois() != null) {
            contrat.setDureeMois(request.getDureeMois());
        }
        if (request.getEtablissement() != null) {
            contrat.setEtablissement(request.getEtablissement());
        }
        if (request.getTuteurNom() != null) {
            contrat.setTuteurNom(request.getTuteurNom());
        }
        if (request.getTuteurEmail() != null) {
            contrat.setTuteurEmail(request.getTuteurEmail());
        }
        if (request.getTuteurTelephone() != null) {
            contrat.setTuteurTelephone(request.getTuteurTelephone());
        }
        if (request.getObjectifsStage() != null) {
            contrat.setObjectifsStage(request.getObjectifsStage());
        }
        if (request.getDureeSemaines() != null) {
            contrat.setDureeSemaines(request.getDureeSemaines());
        }
        if (request.getDescriptionPrestation() != null) {
            contrat.setDescriptionPrestation(request.getDescriptionPrestation());
        }
        if (request.getModalitesPaiement() != null) {
            contrat.setModalitesPaiement(request.getModalitesPaiement());
        }
        if (request.getDureeMoisPrestation() != null) {
            contrat.setDureeMoisPrestation(request.getDureeMoisPrestation());
        }
        if (request.getEstRenouvelable() != null) {
            contrat.setEstRenouvelable(request.getEstRenouvelable());
        }
        if (request.getRenouvellementMax() != null) {
            contrat.setRenouvellementMax(request.getRenouvellementMax());
        }
        if (request.getContratPrecedentId() != null) {
            contrat.setContratPrecedentId(request.getContratPrecedentId());
        }
        if (request.getMotifResiliation() != null) {
            contrat.setMotifResiliation(request.getMotifResiliation());
        }
        if (request.getDateResiliation() != null) {
            contrat.setDateResiliation(request.getDateResiliation());
        }
        if (request.getImageUrls() != null) {
            contrat.setImageUrls(request.getImageUrls());
        }
        if (request.getObservations() != null) {
            contrat.setObservations(request.getObservations());
        }

        // ---- VALIDATION METIER RENFORCEE ----
        LocalDate today = LocalDate.now();
        String nouveauStatut = contrat.getStatut();

        if ("ACTIF".equals(nouveauStatut)) {
            // Si le contrat devient ACTIF, sa date de fin doit être future ou nulle (CDI)
            if (contrat.getDateFin() != null && contrat.getDateFin().isBefore(today)) {
                throw new IllegalArgumentException(
                        "Impossible de passer le contrat en ACTIF avec une date de fin expirée (" + contrat.getDateFin() + "). " +
                                "Veuillez fournir une nouvelle date de fin future ou supprimer la date de fin pour un contrat à durée indéterminée."
                );
            }
            // Si l'ancien statut etait EXPIRE, on logue la reactivation
            if ("EXPIRE".equals(ancienStatut)) {
                log.info("Contrat {} reactivé (EXPIRE -> ACTIF) par {}", id, userId);
            }
        } else if ("EXPIRE".equals(nouveauStatut)) {
            // Optionnel : verifier que la dateFin est bien passee
            if (contrat.getDateFin() != null && contrat.getDateFin().isAfter(today)) {
                log.warn("Contrat {} marque EXPIRE alors que dateFin ({}) est future", id, contrat.getDateFin());
            }
        }

        contrat.setUpdatedAt(LocalDate.now());
        contrat.setUpdatedBy(userId);

        // Vérification critique : l'ID du contrat ne doit pas être null
        if (contrat.getId() == null) {
            log.error("ERREUR CRITIQUE : L'ID du contrat est null avant la sauvegarde pour l'ID fourni {}", id);
            throw new IllegalStateException("L'ID du contrat est null, impossible de mettre à jour. Veuillez vérifier le chargement.");
        }
        log.info("Sauvegarde du contrat avec ID: {}, statut: {}, dateFin: {}", contrat.getId(), contrat.getStatut(), contrat.getDateFin());

        // Sauvegarde
        contratRepository.save(contrat);

        // Recharger pour avoir toutes les modifications
        Contrat saved = getEntity(id);

        // Publication d'evenement de mise a jour
        if (employee != null) {
            publishContractEvent(saved, "UPDATE", userId, employee);
        } else {
            log.warn("Impossible de publier l'evenement pour le contrat {} car l'employe est null", id);
        }

        // Log recapitulatif des modifications
        log.info("Contrat {} modifie : statut {} -> {}, dateFin {} -> {}",
                id, ancienStatut, saved.getStatut(), ancienneDateFin, saved.getDateFin());

        return toDTO(saved, employee);
    }

    // ==========================================
    // RENEW
    // ==========================================
    @Transactional
    public ContratDTO renouvelerContrat(RenouvellementContratRequest request, String userId) {
        Contrat contrat = getEntity(request.getContratId());
        Employee employee = contrat.getEmployee();

        contrat.setDateDebut(LocalDate.now());
        contrat.setDateFin(request.getNouvelleDateFin());
        contrat.setStatut("ACTIF");
        contrat.setUpdatedAt(LocalDate.now());
        contrat.setUpdatedBy(userId);

        contratRepository.save(contrat);
        Contrat saved = getEntity(request.getContratId());

        if (employee != null) {
            publishContractEvent(saved, "RENEW", userId, employee);
        }

        return toDTO(saved, employee);
    }

    // ==========================================
    // RESILIER
    // ==========================================
    @Transactional
    public ContratDTO resillierContrat(String id, String userId) {
        Contrat contrat = getEntity(id);
        Employee employee = contrat.getEmployee();

        contrat.setStatut("EXPIRE");
        contrat.setDateFin(LocalDate.now());
        contrat.setUpdatedAt(LocalDate.now());
        contrat.setUpdatedBy(userId);

        contratRepository.save(contrat);
        Contrat saved = getEntity(id);

        if (employee != null) {
            publishContractEvent(saved, "RESILIATION", userId, employee);
        }

        return toDTO(saved, employee);
    }

    // ==========================================
    // ARCHIVER
    // ==========================================
    @Transactional
    public ContratDTO archiverContrat(String id, String userId) {
        Contrat contrat = getEntity(id);
        Employee employee = contrat.getEmployee();

        contrat.setStatut("ARCHIVE");
        contrat.setUpdatedAt(LocalDate.now());
        contrat.setUpdatedBy(userId);

        contratRepository.save(contrat);
        Contrat saved = getEntity(id);

        if (employee != null) {
            publishContractEvent(saved, "ARCHIVE", userId, employee);
        }

        return toDTO(saved, employee);
    }

    // ==========================================
    // GET ALL
    // ==========================================
    public List<ContratDTO> getAllContrats() {
        return contratRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET BY ID
    // ==========================================
    public ContratDTO getContratById(String id) {
        return toDTO(getEntity(id));
    }

    // ==========================================
    // GET BY EMPLOYEE
    // ==========================================
    public List<ContratDTO> getContratsByEmployee(String employeeId) {
        return contratRepository.findByEmployee_Id(employeeId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // ACTIVE EMPLOYEE CONTRACT
    // ==========================================
    public ContratDTO getActiveContratByEmployee(String employeeId) {
        return contratRepository
                .findByEmployee_IdAndStatut(employeeId, "ACTIF")
                .stream()
                .findFirst()
                .map(this::toDTO)
                .orElse(null);
    }

    // ==========================================
    // FILTERS
    // ==========================================
    public List<ContratDTO> getContratsByStatut(String statut) {
        return contratRepository.findByStatut(statut)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContratDTO> getContratsByType(String type) {
        return contratRepository.findByTypeContrat(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContratDTO> getActiveContracts() {
        return getContratsByStatut("ACTIF");
    }

    public List<ContratDTO> getExpiredContracts() {
        return getContratsByStatut("EXPIRE");
    }

    // ==========================================
    // CONTRATS PROCHES DE L'EXPIRATION
    // ==========================================
    public List<ContratDTO> getContractsExpiringSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);

        return contratRepository
                .findByStatutAndDateFinBetween("ACTIF", today, futureDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContratDTO> getRecentContracts() {
        return contratRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // SEARCH
    // ==========================================
    public List<ContratDTO> searchContrats(String term) {
        String keyword = term.toLowerCase();
        return contratRepository.findAll()
                .stream()
                .filter(c -> c.getTypeContrat() != null &&
                        c.getTypeContrat().toLowerCase().contains(keyword))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // STATISTIQUES
    // ==========================================
    public StatistiquesContratDTO getStatistiques() {
        StatistiquesContratDTO dto = new StatistiquesContratDTO();
        long total = contratRepository.count();
        long actifs = contratRepository.countByStatut("ACTIF");
        long expires = contratRepository.countByStatut("EXPIRE");
        long archives = contratRepository.countByStatut("ARCHIVE");

        dto.setTotalContrats(total);
        dto.setContratsActifs(actifs);
        dto.setContratsExpires(expires);
        dto.setContratsArchives(archives);
        dto.setContratsEnAttente(0);
        dto.setContratsARenouveler(0);

        Map<String, Long> statutMap = new HashMap<>();
        statutMap.put("ACTIF", actifs);
        statutMap.put("EXPIRE", expires);
        statutMap.put("ARCHIVE", archives);
        dto.setParStatut(statutMap);
        dto.setParType(new HashMap<>());
        dto.setTauxActif(total == 0 ? 0 : ((double) actifs / total) * 100);

        return dto;
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private Contrat getEntity(String id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouve avec ID: " + id));
    }

    private String getContentType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }

    // ==========================================
    // METHODES DE PUBLICATION DES NOTIFICATIONS
    // ==========================================

    private void publishContractEvent(Contrat contrat, String action, String userId, Employee employee) {
        if (employee == null) {
            log.warn("Impossible de publier l'evenement pour le contrat {} car l'employe est null", contrat.getId());
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", employee.getId());
        data.put("companyId", employee.getEntrepriseId());
        data.put("departmentId", employee.getDepartementId());
        data.put("entityId", contrat.getId());
        data.put("entityType", "CONTRAT");
        data.put("actionUrl", "/contrats/" + contrat.getId());
        data.put("employeeName", employee.getNom() + " " + (employee.getPrenom() != null ? employee.getPrenom() : ""));
        data.put("endDate", contrat.getDateFin() != null ? contrat.getDateFin().toString() : "N/A");
        data.put("triggeredBy", userId);
        data.put("metadata", Map.of(
                "typeContrat", contrat.getTypeContrat(),
                "statut", contrat.getStatut(),
                "action", action,
                "dateDebut", contrat.getDateDebut().toString(),
                "dateFin", contrat.getDateFin() != null ? contrat.getDateFin().toString() : "N/A"
        ));

        NotificationEvent event;
        switch (action) {
            case "CREATE":
                event = NotificationEvent.CONTRACT_CREATED;
                break;
            case "UPDATE":
                event = NotificationEvent.CONTRACT_UPDATED;
                break;
            case "RENEW":
                event = NotificationEvent.CONTRACT_RENEWED;
                break;
            case "RESILIATION":
                event = NotificationEvent.CONTRACT_RESILIATED;
                break;
            case "ARCHIVE":
                event = NotificationEvent.CONTRACT_ARCHIVED;
                break;
            default:
                event = NotificationEvent.SYSTEM;
                break;
        }

        notificationPublisher.publish(event, data, userId);
    }

    private void publishDocumentUploadEvent(Contrat contrat, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", contrat.getEmployee() != null ? contrat.getEmployee().getId() : null);
        data.put("companyId", contrat.getEmployee() != null ? contrat.getEmployee().getEntrepriseId() : null);
        data.put("departmentId", contrat.getEmployee() != null ? contrat.getEmployee().getDepartementId() : null);
        data.put("entityId", contrat.getId());
        data.put("entityType", "CONTRAT");
        data.put("actionUrl", "/contrats/" + contrat.getId());
        data.put("triggeredBy", userId);
        data.put("metadata", Map.of("action", "DOCUMENT_UPLOADED"));
        notificationPublisher.publish(NotificationEvent.DOCUMENT_UPLOADED, data, userId);
    }
}