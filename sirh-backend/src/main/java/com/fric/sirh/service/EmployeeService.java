package com.fric.sirh.service;

import com.fric.sirh.dto.EmployeeAdminUpdateRequest;
import com.fric.sirh.dto.EmployeeFullCreationRequest;
import com.fric.sirh.dto.EmployeeSelfUpdateRequest;
import com.fric.sirh.model.Contrat;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Documents;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.ContratRepository;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.DocumentsRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ContratRepository contratRepository;
    private final DocumentsRepository documentsRepository;
    private final DepartementRepository departementRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final NotificationPublisher notificationPublisher;

    // ==========================================
    // RECHERCHE PAR USER ID
    // ==========================================

    public Employee findByUserId(String userId) {
        log.info("Recherche d'employe par userId: {}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("userId null ou vide");
            return null;
        }

        try {
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("Utilisateur non trouve avec l'ID: {}", userId);
                return null;
            }

            User user = userOpt.get();

            String employeeId = user.getEmployeeId();

            if (employeeId == null || employeeId.trim().isEmpty()) {
                log.warn("L'utilisateur {} n'a pas d'employeeId associe", userId);
                return null;
            }

            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);

            if (employeeOpt.isEmpty()) {
                log.warn("Employe non trouve avec l'ID: {}", employeeId);
                return null;
            }

            Employee employee = employeeOpt.get();

            log.info(
                    "Employe trouve par userId: {} - {} {}",
                    userId,
                    employee.getPrenom(),
                    employee.getNom()
            );

            return employee;

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recherche par userId: {}",
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    // ==========================================
    // RECHERCHE PAR ID
    // ==========================================

    public Employee getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'identifiant de l'employe est obligatoire.");
        }

        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employe introuvable avec l'id : " + id
                        )
                );
    }

    // ==========================================
    // RECHERCHE PAR MATRICULE
    // ==========================================

    public Employee findByMatricule(String matricule) {
        log.info("Recherche d'employe par matricule: {}", matricule);

        if (matricule == null || matricule.trim().isEmpty()) {
            log.warn("Matricule null ou vide");
            return null;
        }

        try {
            Optional<Employee> employeeOpt =
                    employeeRepository.findByMatriculeInterne(matricule.trim());

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();

                log.info(
                        "Employe trouve par matricule: {} - {} {}",
                        matricule,
                        employee.getPrenom(),
                        employee.getNom()
                );

                return employee;
            }

            log.warn(
                    "Aucun employe trouve avec le matricule: {}",
                    matricule
            );

            return null;

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recherche par matricule: {}",
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    // ==========================================
    // RECHERCHE PAR EMAIL
    // ==========================================

    public Employee findByUserEmail(String email) {
        log.info("Recherche d'employe par email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Email null ou vide");
            return null;
        }

        try {
            Optional<User> userOpt =
                    userRepository.findByEmail(email.trim());

            if (userOpt.isEmpty()) {
                log.warn(
                        "Utilisateur non trouve avec l'email: {}",
                        email
                );
                return null;
            }

            User user = userOpt.get();

            String employeeId = user.getEmployeeId();

            if (employeeId == null || employeeId.trim().isEmpty()) {
                log.warn(
                        "L'utilisateur {} n'a pas d'employeeId associe",
                        email
                );
                return null;
            }

            Optional<Employee> employeeOpt =
                    employeeRepository.findById(employeeId);

            if (employeeOpt.isEmpty()) {
                log.warn(
                        "Employe non trouve avec l'ID: {}",
                        employeeId
                );
                return null;
            }

            Employee employee = employeeOpt.get();

            log.info(
                    "Employe trouve par email: {} - {} {}",
                    email,
                    employee.getPrenom(),
                    employee.getNom()
            );

            return employee;

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recherche par email: {}",
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    // ==========================================
    // FILTRAGE PAR ENTREPRISE
    // ==========================================

    public List<Employee> getEmployeesBySameCompany(String departementId) {

        if (departementId == null || departementId.isEmpty()) {
            log.warn(
                    "Tentative de recuperation des employes avec un departement null"
            );
            return List.of();
        }

        try {
            Optional<Departement> deptOpt =
                    departementRepository.findById(departementId);

            if (deptOpt.isEmpty()) {
                log.warn(
                        "Departement non trouve avec l'ID: {}",
                        departementId
                );
                return List.of();
            }

            String entrepriseId = deptOpt.get().getEntrepriseId();

            if (entrepriseId == null || entrepriseId.isEmpty()) {
                log.warn(
                        "Le departement {} n'a pas d'entreprise associee",
                        departementId
                );
                return List.of();
            }

            List<Departement> departements =
                    departementRepository.findByEntrepriseId(entrepriseId);

            if (departements.isEmpty()) {
                log.warn(
                        "Aucun departement trouve pour l'entreprise {}",
                        entrepriseId
                );
                return List.of();
            }

            List<String> departementIds = departements.stream()
                    .map(Departement::getId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toList());

            if (departementIds.isEmpty()) {
                return List.of();
            }

            List<Employee> employees =
                    employeeRepository.findByDepartementIdIn(departementIds);

            log.info(
                    "{} employes trouves pour l'entreprise {}",
                    employees.size(),
                    entrepriseId
            );

            return employees;

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recuperation des employes par entreprise: {}",
                    e.getMessage(),
                    e
            );
            return List.of();
        }
    }

    public boolean areEmployeesInSameCompany(
            String departementId1,
            String departementId2
    ) {

        if (departementId1 == null || departementId2 == null) {
            return false;
        }

        try {
            Optional<Departement> dept1 =
                    departementRepository.findById(departementId1);

            if (dept1.isEmpty()) {
                return false;
            }

            String entrepriseId1 = dept1.get().getEntrepriseId();

            Optional<Departement> dept2 =
                    departementRepository.findById(departementId2);

            if (dept2.isEmpty()) {
                return false;
            }

            String entrepriseId2 = dept2.get().getEntrepriseId();

            if (entrepriseId1 == null || entrepriseId2 == null) {
                return false;
            }

            return entrepriseId1.equals(entrepriseId2);

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la verification de l'entreprise commune: {}",
                    e.getMessage(),
                    e
            );
            return false;
        }
    }

    public String getEntrepriseIdByDepartementId(String departementId) {

        if (departementId == null || departementId.isEmpty()) {
            return null;
        }

        try {
            Optional<Departement> deptOpt =
                    departementRepository.findById(departementId);

            return deptOpt
                    .map(Departement::getEntrepriseId)
                    .orElse(null);

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recuperation de l'entreprise: {}",
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    public String getEntrepriseIdByEmployeeId(String employeeId) {

        if (employeeId == null || employeeId.isEmpty()) {
            return null;
        }

        try {
            Employee employee = getById(employeeId);

            if (employee.getDepartementId() == null) {
                return null;
            }

            return getEntrepriseIdByDepartementId(
                    employee.getDepartementId()
            );

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la recuperation de l'entreprise: {}",
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    // ==========================================
    // FILTRAGE
    // ==========================================

    public List<Employee> getEmployeesByDepartement(String departementId) {

        if (departementId == null || departementId.isEmpty()) {
            log.warn(
                    "Tentative de recuperation des employes avec un departement null"
            );
            return List.of();
        }

        log.info(
                "Recuperation des employes du departement: {}",
                departementId
        );

        List<Employee> employees =
                employeeRepository.findByDepartementId(departementId);

        log.info(
                "{} employes trouves dans le departement {}",
                employees.size(),
                departementId
        );

        return employees;
    }

    public List<Employee> getEmployeesByEntreprise(String entrepriseId) {

        if (entrepriseId == null || entrepriseId.isEmpty()) {
            log.warn("Entreprise ID null ou vide");
            return List.of();
        }

        log.info(
                "Recuperation des employes de l'entreprise: {}",
                entrepriseId
        );

        List<Departement> departements =
                departementRepository.findByEntrepriseId(entrepriseId);

        if (departements.isEmpty()) {
            log.warn(
                    "Aucun departement trouve pour l'entreprise {}",
                    entrepriseId
            );
            return List.of();
        }

        List<String> departementIds = departements.stream()
                .map(Departement::getId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        if (departementIds.isEmpty()) {
            return List.of();
        }

        List<Employee> employees =
                employeeRepository.findByDepartementIdIn(departementIds);

        log.info(
                "{} employes trouves pour l'entreprise {}",
                employees.size(),
                entrepriseId
        );

        return employees;
    }

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> search(String term) {

        log.info(
                "Recherche d'employes avec le terme: {}",
                term
        );

        if (term == null || term.trim().isEmpty()) {
            return employeeRepository.findAll();
        }

        String searchTerm = term.toLowerCase().trim();

        List<Employee> employees =
                employeeRepository.findAll();

        return employees.stream()
                .filter(emp -> {

                    String nom =
                            emp.getNom() != null
                                    ? emp.getNom().toLowerCase()
                                    : "";

                    String prenom =
                            emp.getPrenom() != null
                                    ? emp.getPrenom().toLowerCase()
                                    : "";

                    String email =
                            emp.getUser() != null
                                    && emp.getUser().getEmail() != null
                                    ? emp.getUser().getEmail().toLowerCase()
                                    : "";

                    String matricule =
                            emp.getMatriculeInterne() != null
                                    ? emp.getMatriculeInterne().toLowerCase()
                                    : "";

                    String telephone =
                            emp.getTelephone() != null
                                    ? emp.getTelephone().toLowerCase()
                                    : "";

                    String numeroUrgence =
                            emp.getNumeroContactUrgence() != null
                                    ? emp.getNumeroContactUrgence().toLowerCase()
                                    : "";

                    return nom.contains(searchTerm)
                            || prenom.contains(searchTerm)
                            || email.contains(searchTerm)
                            || matricule.contains(searchTerm)
                            || telephone.contains(searchTerm)
                            || numeroUrgence.contains(searchTerm);
                })
                .collect(Collectors.toList());
    }

    public List<Employee> searchInCompany(
            String term,
            String departementId
    ) {

        log.info(
                "Recherche d'employes dans l'entreprise avec le terme: {}",
                term
        );

        if (departementId == null || departementId.isEmpty()) {
            return List.of();
        }

        List<Employee> companyEmployees =
                getEmployeesBySameCompany(departementId);

        if (term == null || term.trim().isEmpty()) {
            return companyEmployees;
        }

        String searchTerm = term.toLowerCase().trim();

        return companyEmployees.stream()
                .filter(emp -> {

                    String nom =
                            emp.getNom() != null
                                    ? emp.getNom().toLowerCase()
                                    : "";

                    String prenom =
                            emp.getPrenom() != null
                                    ? emp.getPrenom().toLowerCase()
                                    : "";

                    String email =
                            emp.getUser() != null
                                    && emp.getUser().getEmail() != null
                                    ? emp.getUser().getEmail().toLowerCase()
                                    : "";

                    String matricule =
                            emp.getMatriculeInterne() != null
                                    ? emp.getMatriculeInterne().toLowerCase()
                                    : "";

                    String telephone =
                            emp.getTelephone() != null
                                    ? emp.getTelephone().toLowerCase()
                                    : "";

                    String numeroUrgence =
                            emp.getNumeroContactUrgence() != null
                                    ? emp.getNumeroContactUrgence().toLowerCase()
                                    : "";

                    return nom.contains(searchTerm)
                            || prenom.contains(searchTerm)
                            || email.contains(searchTerm)
                            || matricule.contains(searchTerm)
                            || telephone.contains(searchTerm)
                            || numeroUrgence.contains(searchTerm);
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // CREATION D'UN EMPLOYE
    // ==========================================

    @Transactional
    public Employee createEmployeeWithAccount(
            EmployeeFullCreationRequest request,
            List<MultipartFile> files,
            String creator
    ) {

        log.info(
                "Creation d'un nouvel employe: {}",
                request.getEmail()
        );

        validateEmployeeCreation(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Cet email est deja associe a un compte utilisateur."
            );
        }

        if (employeeRepository.existsByMatriculeInterne(
                request.getMatricule_interne()
        )) {
            throw new IllegalArgumentException(
                    "Le matricule interne est deja utilise."
            );
        }

        if (request.getMatricule_CNPS() != null
                && !request.getMatricule_CNPS().trim().isEmpty()
                && Boolean.TRUE.equals(employeeRepository.existsByMatriculeCNPS(
                request.getMatricule_CNPS()
        ))) {
            throw new IllegalArgumentException(
                    "Le matricule CNPS est deja utilise."
            );
        }

        User user = User.builder()
                .firstName(request.getPrenom())
                .lastName(request.getNom())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(request.getPassword())
                )
                .roleId(request.getRoleId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .createdBy(creator)
                .loginAttempts(0)
                .locked(false)
                .build();

        User savedUser = userRepository.save(user);

        Employee employee = new Employee();

        employee.setNom(request.getNom());
        employee.setPrenom(request.getPrenom());
        employee.setMatriculeInterne(
                request.getMatricule_interne()
        );
        employee.setMatricule_CNPS(
                normalizeOptionalValue(request.getMatricule_CNPS())
        );
        employee.setSexe(request.getSexe());
        employee.setNombreEnfantsMoinsDe7Ans(
                request.getNombreEnfantsMoinsDe7Ans()
        );
        employee.setDate_naissance(
                request.getDate_naissance()
        );
        employee.setTelephone(request.getTelephone());
        employee.setNumeroContactUrgence(
                request.getNumeroContactUrgence()
        );
        employee.setAddresse(request.getAddresse());
        employee.setDate_embauche(
                request.getDate_embauche()
        );
        employee.setPosteId(request.getPosteId());
        employee.setDepartementId(request.getDepartementId());
        employee.setEntrepriseId(request.getEntrepriseId());
        employee.setStatut("ACTIF");
        employee.setCreatedAt(LocalDate.now());
        employee.setCreatedBy(creator);
        employee.setUser(savedUser);

        Employee savedEmployee =
                employeeRepository.save(employee);

        savedUser.setEmployeeId(savedEmployee.getId());

        userRepository.save(savedUser);

        // ==========================================
        // CREATION DU CONTRAT
        // ==========================================

        if (request.getTypeContrat() != null
                && !request.getTypeContrat().trim().isEmpty()) {

            Contrat contrat = new Contrat();

            contrat.setEmployee(savedEmployee);

            contrat.setTypeContrat(
                    request.getTypeContrat()
            );

            contrat.setDateDebut(
                    request.getDateDebutContrat()
            );

            contrat.setDateFin(
                    request.getDateFinContrat()
            );

            contrat.setDateFinEssai(
                    request.getDateFinEssai()
            );

            contrat.setDureeEssaiMois(
                    request.getDureeEssaiMois()
            );

            // Remuneration
            contrat.setSalaireBrut(
                    request.getSalaireBrut()
            );

            contrat.setSalaireNet(
                    request.getSalaireNet()
            );

            contrat.setTauxHoraire(
                    request.getTauxHoraire()
            );

            contrat.setNombreHeuresSemaine(
                    request.getNombreHeuresSemaine()
            );

            // CDD
            contrat.setMotifRecours(
                    request.getMotifRecours()
            );

            contrat.setDureeMois(
                    request.getDureeMois()
            );

            // Stage
            contrat.setEtablissement(
                    request.getEtablissement()
            );

            contrat.setTuteurNom(
                    request.getTuteurNom()
            );

            contrat.setTuteurEmail(
                    request.getTuteurEmail()
            );

            contrat.setTuteurTelephone(
                    request.getTuteurTelephone()
            );

            contrat.setObjectifsStage(
                    request.getObjectifsStage()
            );

            contrat.setDureeSemaines(
                    request.getDureeSemaines()
            );

            // Freelance
            contrat.setDescriptionPrestation(
                    request.getDescriptionPrestation()
            );

            contrat.setModalitesPaiement(
                    request.getModalitesPaiement()
            );

            contrat.setDureeMoisPrestation(
                    request.getDureeMoisPrestation()
            );

            // Renouvellement
            if (request.getEstRenouvelable() != null) {
                contrat.setEstRenouvelable(
                        request.getEstRenouvelable()
                );
            }

            contrat.setRenouvellementMax(
                    request.getRenouvellementMax()
            );

            contrat.setStatut("ACTIF");

            contrat.setCreatedAt(LocalDate.now());

            contrat.setCreatedBy(creator);

            contrat.setImageUrls(new ArrayList<>());

            contrat.setVersion(1);

            contratRepository.save(contrat);

            processAndSaveDocument(
                    request.getContratUrls(),
                    "CONTRAT_SIGNE",
                    "Contrat_" + request.getNom(),
                    savedEmployee,
                    contrat,
                    creator
            );
        }

        // ==========================================
        // FICHIERS EMPLOYE
        // ==========================================

        if (files != null && !files.isEmpty()) {
            processAndSaveFilesWithStorage(
                    files,
                    savedEmployee,
                    null,
                    creator
            );
        }

        processAndSaveDocument(
                request.getCniUrls(),
                "CNI",
                "CNI_" + request.getNom(),
                savedEmployee,
                null,
                creator
        );

        processAndSaveDocument(
                request.getCertificatUrls(),
                "CERTIFICAT",
                "Certificat_" + request.getNom(),
                savedEmployee,
                null,
                creator
        );

        processAndSaveDocument(
                request.getPhotoUrls(),
                "PHOTO",
                "Photo_" + request.getNom(),
                savedEmployee,
                null,
                creator
        );

        publishEmployeeEvent(
                savedEmployee,
                NotificationEvent.EMPLOYEE_CREATED,
                creator
        );

        log.info(
                "Employe cree avec succes: {}",
                savedEmployee.getId()
        );

        return savedEmployee;
    }

    // ==========================================
    // VALIDATION CREATION
    // ==========================================

    private void validateEmployeeCreation(
            EmployeeFullCreationRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Les donnees de creation de l'employe sont obligatoires."
            );
        }

        if (isBlank(request.getNom())) {
            throw new IllegalArgumentException(
                    "Le nom est obligatoire."
            );
        }

        if (isBlank(request.getPrenom())) {
            throw new IllegalArgumentException(
                    "Le prenom est obligatoire."
            );
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException(
                    "L'email est obligatoire."
            );
        }

        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException(
                    "Le mot de passe est obligatoire."
            );
        }

        if (isBlank(request.getMatricule_interne())) {
            throw new IllegalArgumentException(
                    "Le matricule interne est obligatoire."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeOptionalValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    // ==========================================
    // MISE A JOUR DU PROFIL
    // ==========================================

    @Transactional
    public Employee updateSelfProfile(
            String id,
            EmployeeSelfUpdateRequest request
    ) {

        log.info(
                "Mise a jour du profil pour employeeId: {}",
                id
        );

        Employee existing = getById(id);

        if (request.getTelephone() != null) {
            existing.setTelephone(request.getTelephone());
        }

        if (request.getNumeroContactUrgence() != null) {   // AJOUTÉ
            existing.setNumeroContactUrgence(request.getNumeroContactUrgence());
        }

        if (request.getAddresse() != null) {
            existing.setAddresse(request.getAddresse());
        }

        existing.setUpdatedAt(LocalDate.now());

        Employee saved =
                employeeRepository.save(existing);

        log.info(
                "Profil mis a jour pour: {} {}",
                saved.getPrenom(),
                saved.getNom()
        );

        return saved;
    }

    // ==========================================
    // MISE A JOUR ADMIN
    // ==========================================

    @Transactional
    public Employee updateByAdmin(
            String id,
            EmployeeAdminUpdateRequest request,
            String updaterId
    ) {

        Employee existing = getById(id);

        if (request.getNom() != null) {
            existing.setNom(request.getNom());
        }

        if (request.getPrenom() != null) {
            existing.setPrenom(request.getPrenom());
        }

        if (request.getMatricule_CNPS() != null) {

            String matriculeCNPS =
                    normalizeOptionalValue(
                            request.getMatricule_CNPS()
                    );

            if (matriculeCNPS != null
                    && Boolean.TRUE.equals(employeeRepository.existsByMatriculeCNPSAndIdNot(
                    matriculeCNPS,
                    id
            ))) {

                throw new IllegalArgumentException(
                        "Le matricule CNPS est deja utilise."
                );
            }

            existing.setMatricule_CNPS(matriculeCNPS);
        }

        if (request.getSexe() != null) {
            existing.setSexe(request.getSexe());
        }

        if (request.getDate_naissance() != null) {
            existing.setDate_naissance(
                    request.getDate_naissance()
            );
        }

        if (request.getTelephone() != null) {
            existing.setTelephone(
                    request.getTelephone()
            );
        }

        if (request.getAddresse() != null) {
            existing.setAddresse(
                    request.getAddresse()
            );
        }

        if (request.getDate_embauche() != null) {
            existing.setDate_embauche(
                    request.getDate_embauche()
            );
        }

        if (request.getPosteId() != null) {
            existing.setPosteId(
                    request.getPosteId()
            );
        }

        if (request.getDepartementId() != null) {
            existing.setDepartementId(
                    request.getDepartementId()
            );
        }

        if (request.getEntrepriseId() != null) {
            existing.setEntrepriseId(
                    request.getEntrepriseId()
            );
        }

        if (request.getStatut() != null) {
            existing.setStatut(
                    request.getStatut()
            );
        }

        existing.setUpdatedAt(LocalDate.now());

        existing.setUpdatedBy(updaterId);

        Employee saved =
                employeeRepository.save(existing);

        publishEmployeeEvent(
                saved,
                NotificationEvent.EMPLOYEE_UPDATED,
                updaterId
        );

        log.info(
                "Employe mis a jour: {}",
                saved.getId()
        );

        return saved;
    }

    // ==========================================
    // SUSPENSION
    // ==========================================

    @Transactional
    public Employee suspendre(
            String id,
            String updaterId
    ) {

        Employee employee = getById(id);

        employee.setStatut("SUSPENDU");

        employee.setUpdatedAt(LocalDate.now());

        employee.setUpdatedBy(updaterId);

        Employee saved =
                employeeRepository.save(employee);

        if (saved.getUser() != null) {

            User user = saved.getUser();

            user.setActive(false);

            user.setUpdatedAt(
                    LocalDateTime.now()
            );

            user.setUpdatedBy(updaterId);

            userRepository.save(user);
        }

        publishEmployeeEvent(
                saved,
                NotificationEvent.EMPLOYEE_SUSPENDED,
                updaterId
        );

        log.info(
                "Employe suspendu: {}",
                saved.getId()
        );

        return saved;
    }

    // ==========================================
    // REACTIVATION
    // ==========================================

    @Transactional
    public Employee reactiver(
            String id,
            String updaterId
    ) {

        Employee employee = getById(id);

        employee.setStatut("ACTIF");

        employee.setUpdatedAt(LocalDate.now());

        employee.setUpdatedBy(updaterId);

        Employee saved =
                employeeRepository.save(employee);

        if (saved.getUser() != null) {

            User user = saved.getUser();

            user.setActive(true);

            user.setUpdatedAt(
                    LocalDateTime.now()
            );

            user.setUpdatedBy(updaterId);

            userRepository.save(user);
        }

        publishEmployeeEvent(
                saved,
                NotificationEvent.EMPLOYEE_REACTIVATED,
                updaterId
        );

        log.info(
                "Employe reactive: {}",
                saved.getId()
        );

        return saved;
    }

    // ==========================================
    // SUPPRESSION
    // ==========================================

    @Transactional
    public void deleteEmployee(
            String id,
            String deleterId
    ) {

        Employee employee = getById(id);

        List<Documents> documents =
                documentsRepository.findByEmployee_Id(id);

        for (Documents doc : documents) {

            if (doc.getImageUrls() != null) {

                for (String url : doc.getImageUrls()) {

                    if (url != null
                            && !url.trim().isEmpty()) {

                        fileStorageService.deleteFile(url);
                    }
                }
            }
        }

        if (!documents.isEmpty()) {
            documentsRepository.deleteAll(documents);
        }

        List<Contrat> contrats =
                contratRepository.findByEmployee_Id(id);

        if (!contrats.isEmpty()) {
            contratRepository.deleteAll(contrats);
        }

        employeeRepository.delete(employee);

        if (employee.getUser() != null) {
            userRepository.delete(employee.getUser());
        }

        log.info(
                "Employe {} supprime par {}",
                id,
                deleterId
        );
    }

    // ==========================================
    // TRAITEMENT DES FICHIERS
    // ==========================================

    private void processAndSaveFilesWithStorage(
            List<MultipartFile> files,
            Employee employee,
            Contrat contrat,
            String creator
    ) {

        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                continue;
            }

            try {

                String originalFilename =
                        file.getOriginalFilename();

                if (originalFilename == null
                        || originalFilename.trim().isEmpty()) {
                    continue;
                }

                String prefix =
                        originalFilename
                                .split("_")[0]
                                .toUpperCase();

                String type;

                switch (prefix) {

                    case "CNI":
                        type = "CNI";
                        break;

                    case "CONTRAT":
                        type = "CONTRAT_SIGNE";
                        break;

                    case "DIPLOME":
                        type = "DIPLOME";
                        break;

                    case "PHOTO":
                        type = "PHOTO";
                        break;

                    case "CERTIFICAT":
                        type = "CERTIFICAT";
                        break;

                    default:
                        type = "AUTRE";
                        break;
                }

                String subDirectory;

                switch (type) {

                    case "CNI":
                        subDirectory = "cni";
                        break;

                    case "CONTRAT_SIGNE":
                        subDirectory = "contrats";
                        break;

                    case "DIPLOME":
                        subDirectory = "diplomes";
                        break;

                    case "PHOTO":
                        subDirectory = "photos";
                        break;

                    case "CERTIFICAT":
                        subDirectory = "certificats";
                        break;

                    default:
                        subDirectory = "documents";
                        break;
                }

                String fileUrl =
                        fileStorageService.storeFile(
                                file,
                                subDirectory
                        );

                List<String> urls =
                        Collections.singletonList(fileUrl);

                Documents doc = new Documents();

                doc.setName(type);

                doc.setTypeDocument(type);

                doc.setImageUrls(urls);

                doc.setDateUpload(LocalDate.now());

                doc.setEmployee(employee);

                if (contrat != null
                        && "CONTRAT_SIGNE".equals(type)) {

                    doc.setContrat(contrat);
                }

                doc.setCreatedAt(LocalDate.now());

                doc.setCreatedBy(creator);

                documentsRepository.save(doc);

                log.info(
                        "Document sauvegarde: {}",
                        fileUrl
                );

            } catch (Exception e) {

                log.error(
                        "Erreur lors du traitement du fichier: {}",
                        e.getMessage(),
                        e
                );

                throw new RuntimeException(
                        "Impossible de traiter le fichier: "
                                + file.getOriginalFilename(),
                        e
                );
            }
        }
    }

    private void processAndSaveDocument(
            List<String> urls,
            String typeDoc,
            String docName,
            Employee employee,
            Contrat contrat,
            String creator
    ) {

        if (urls == null || urls.isEmpty()) {
            return;
        }

        List<String> validUrls =
                urls.stream()
                        .filter(url ->
                                url != null
                                        && !url.trim().isEmpty()
                        )
                        .collect(Collectors.toList());

        if (validUrls.isEmpty()) {
            return;
        }

        Documents doc = new Documents();

        doc.setName(docName);

        doc.setTypeDocument(typeDoc);

        doc.setImageUrls(validUrls);

        doc.setDateUpload(LocalDate.now());

        doc.setEmployee(employee);

        if (contrat != null) {
            doc.setContrat(contrat);
        }

        doc.setCreatedAt(LocalDate.now());

        doc.setCreatedBy(creator);

        documentsRepository.save(doc);

        log.info(
                "Document sauvegarde: {}",
                typeDoc
        );
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================

    private void publishEmployeeEvent(
            Employee employee,
            NotificationEvent event,
            String userId
    ) {

        try {

            if (employee == null) {
                log.warn(
                        "Employee est null, impossible de publier l'evenement"
                );
                return;
            }

            Map<String, Object> data =
                    new HashMap<>();

            String employeeId =
                    employee.getId() != null
                            ? employee.getId()
                            : "";

            data.put(
                    "employeeId",
                    employeeId
            );

            data.put(
                    "companyId",
                    employee.getEntrepriseId() != null
                            ? employee.getEntrepriseId()
                            : ""
            );

            data.put(
                    "departmentId",
                    employee.getDepartementId() != null
                            ? employee.getDepartementId()
                            : ""
            );

            data.put(
                    "entityId",
                    employeeId
            );

            data.put(
                    "entityType",
                    "EMPLOYEE"
            );

            data.put(
                    "actionUrl",
                    "/employees/" + employeeId
            );

            Map<String, Object> metadata =
                    new HashMap<>();

            metadata.put(
                    "nom",
                    employee.getNom() != null
                            ? employee.getNom()
                            : ""
            );

            metadata.put(
                    "prenom",
                    employee.getPrenom() != null
                            ? employee.getPrenom()
                            : ""
            );

            metadata.put(
                    "statut",
                    employee.getStatut() != null
                            ? employee.getStatut()
                            : ""
            );

            data.put(
                    "metadata",
                    metadata
            );

            notificationPublisher.publish(
                    event,
                    data,
                    userId
            );

        } catch (Exception e) {

            log.error(
                    "Erreur lors de la publication de l'evenement: {}",
                    e.getMessage(),
                    e
            );
        }
    }
}