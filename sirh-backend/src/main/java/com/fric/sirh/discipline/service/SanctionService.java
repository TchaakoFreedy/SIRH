package com.fric.sirh.discipline.service;

import com.fric.sirh.discipline.dto.DashboardSanctionStats;
import com.fric.sirh.discipline.dto.SanctionDTO;
import com.fric.sirh.discipline.enums.StatutSanction;
import com.fric.sirh.discipline.enums.TypeActionHistorique;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.model.Sanction;
import com.fric.sirh.discipline.model.HistoriqueDiscipline;
import com.fric.sirh.discipline.repository.SanctionRepository;
import com.fric.sirh.discipline.repository.DemandeExplicationRepository;
import com.fric.sirh.discipline.mapper.SanctionMapper;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.DepartementRepository;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SanctionService {

    private final SanctionRepository sanctionRepository;
    private final DemandeExplicationRepository demandeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartementRepository departementRepository;
    private final RoleRepository roleRepository;
    private final SanctionMapper sanctionMapper;

    public SanctionService(SanctionRepository sanctionRepository,
                           DemandeExplicationRepository demandeRepository,
                           EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           DepartementRepository departementRepository,
                           RoleRepository roleRepository,
                           SanctionMapper sanctionMapper) {
        this.sanctionRepository = sanctionRepository;
        this.demandeRepository = demandeRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departementRepository = departementRepository;
        this.roleRepository = roleRepository;
        this.sanctionMapper = sanctionMapper;
    }

    // ============================================
    // VISIBILITE PAR ROLE (existant)
    // ============================================

    public Page<SanctionDTO> getSanctionsByRole(String userId, Pageable pageable) {
        log.info("Recuperation des sanctions pour l'utilisateur: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve: " + userId));

        String roleId = user.getRoleId();
        log.info("roleId de l'utilisateur: {}", roleId);

        String roleName = "";
        if (roleId != null) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                roleName = role.getName();
                log.info("Nom du role: {}", roleName);
            }
        }

        if ("RH".equals(roleName) || "TOP_MANAGER".equals(roleName)) {
            log.info("RH/TOP_MANAGER - Acces a toutes les sanctions");
            return sanctionRepository.findAll(pageable).map(sanctionMapper::toDTO);
        }

        Employee employee = getOrCreateEmployee(user);
        if (employee == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}, retour page vide", userId);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        if ("DIRECTION".equals(roleName)) {
            String departmentId = employee.getDepartementId();
            log.info("DIRECTION - Departement ID: {}", departmentId);

            if (departmentId == null || departmentId.isEmpty()) {
                log.warn("L'employe n'a pas de departement associe");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            Departement departement = departementRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Departement non trouve: " + departmentId));

            String companyId = departement.getEntrepriseId();
            log.info("DIRECTION - Entreprise ID (via departement): {}", companyId);

            if (companyId == null || companyId.isEmpty()) {
                log.warn("Le departement n'a pas d'entreprise associee");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            List<Employee> companyEmployees = employeeRepository.findByEntrepriseId(companyId);
            List<String> employeeIds = companyEmployees.stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());

            if (employeeIds.isEmpty()) {
                log.warn("Aucun employe trouve dans l'entreprise: {}", companyId);
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            List<Sanction> sanctions = sanctionRepository.findByEmployeIdIn(employeeIds);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), sanctions.size());

            List<SanctionDTO> sanctionDTOs = sanctions.stream()
                    .skip(start)
                    .limit(pageable.getPageSize())
                    .map(sanctionMapper::toDTO)
                    .collect(Collectors.toList());

            log.info("{} sanctions trouvees pour l'entreprise: {}", sanctionDTOs.size(), companyId);
            return new PageImpl<>(sanctionDTOs, pageable, sanctions.size());
        }

        if ("MANAGER".equals(roleName)) {
            String departmentId = employee.getDepartementId();
            log.info("MANAGER - Departement ID: {}", departmentId);

            if (departmentId == null || departmentId.isEmpty()) {
                log.warn("L'employe n'a pas de departement associe");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            List<Employee> departmentEmployees = employeeRepository.findByDepartementId(departmentId);
            List<String> employeeIds = departmentEmployees.stream()
                    .map(Employee::getId)
                    .collect(Collectors.toList());

            if (employeeIds.isEmpty()) {
                log.warn("Aucun employe trouve dans le departement: {}", departmentId);
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            List<Sanction> sanctions = sanctionRepository.findByEmployeIdIn(employeeIds);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), sanctions.size());

            List<SanctionDTO> sanctionDTOs = sanctions.stream()
                    .skip(start)
                    .limit(pageable.getPageSize())
                    .map(sanctionMapper::toDTO)
                    .collect(Collectors.toList());

            log.info("{} sanctions trouvees pour le departement: {}", sanctionDTOs.size(), departmentId);
            return new PageImpl<>(sanctionDTOs, pageable, sanctions.size());
        }

        log.info("EMPLOYEE - Acces a ses propres sanctions");
        return sanctionRepository.findByEmploye_Id(employee.getId(), pageable).map(sanctionMapper::toDTO);
    }

    // ============================================
    // NOUVEAU : Dashboard Manager (departement)
    // ============================================

    public Page<SanctionDTO> getDashboardManagerSanctions(String userId, Pageable pageable) {
        log.info("Dashboard Manager - Recuperation des sanctions pour l'utilisateur: {}", userId);
        return getSanctionsByRole(userId, pageable);
    }

    // ============================================
    // NOUVEAU : Dashboard Direction (entreprise)
    // ============================================

    public Page<SanctionDTO> getDashboardDirectionSanctions(String userId, Pageable pageable) {
        log.info("Dashboard Direction - Recuperation des sanctions pour l'utilisateur: {}", userId);
        return getSanctionsByRole(userId, pageable);
    }

    // ============================================
    // NOUVEAU : Statistiques pour le dashboard (CORRIGE)
    // ============================================

    public DashboardSanctionStats getDashboardStats(String userId) {
        log.info("Dashboard Stats pour l'utilisateur: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve: " + userId));

        String roleId = user.getRoleId();
        String roleName = "";
        if (roleId != null) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                roleName = role.getName();
            }
        }

        List<Sanction> sanctions = new ArrayList<>();

        if ("RH".equals(roleName) || "TOP_MANAGER".equals(roleName)) {
            sanctions = sanctionRepository.findAll();
        } else {
            Employee employee = getOrCreateEmployee(user);
            if (employee == null) {
                return emptyStats();
            }

            if ("DIRECTION".equals(roleName)) {
                String departmentId = employee.getDepartementId();
                if (departmentId == null || departmentId.isEmpty()) {
                    return emptyStats();
                }
                Departement departement = departementRepository.findById(departmentId)
                        .orElseThrow(() -> new RuntimeException("Departement non trouve: " + departmentId));
                String companyId = departement.getEntrepriseId();
                if (companyId == null || companyId.isEmpty()) {
                    return emptyStats();
                }
                List<Employee> companyEmployees = employeeRepository.findByEntrepriseId(companyId);
                List<String> employeeIds = companyEmployees.stream()
                        .map(Employee::getId)
                        .collect(Collectors.toList());
                if (!employeeIds.isEmpty()) {
                    sanctions = sanctionRepository.findByEmployeIdIn(employeeIds);
                }
            } else if ("MANAGER".equals(roleName)) {
                String departmentId = employee.getDepartementId();
                if (departmentId == null || departmentId.isEmpty()) {
                    return emptyStats();
                }
                List<Employee> departmentEmployees = employeeRepository.findByDepartementId(departmentId);
                List<String> employeeIds = departmentEmployees.stream()
                        .map(Employee::getId)
                        .collect(Collectors.toList());
                if (!employeeIds.isEmpty()) {
                    sanctions = sanctionRepository.findByEmployeIdIn(employeeIds);
                }
            } else {
                sanctions = sanctionRepository.findByEmploye_Id(employee.getId());
            }
        }

        DashboardSanctionStats stats = new DashboardSanctionStats();
        stats.setTotal(sanctions.size());

        long actives = sanctions.stream()
                .filter(s -> s.getStatut() == StatutSanction.ACTIVE)
                .count();
        stats.setActives(actives);

        // Les sanctions "levees" correspondent à celles dont le statut est ANNULEE
        // (la méthode leverSanction utilise ANNULEE)
        long levees = sanctions.stream()
                .filter(s -> s.getStatut() == StatutSanction.ANNULEE)
                .count();
        stats.setLevees(levees);

        long annulees = sanctions.stream()
                .filter(s -> s.getStatut() == StatutSanction.ANNULEE)
                .count();
        stats.setAnnulees(annulees);

        Map<String, Long> parType = sanctions.stream()
                .collect(Collectors.groupingBy(s -> s.getType().name(), Collectors.counting()));
        stats.setParType(parType);

        Map<String, Long> parStatut = sanctions.stream()
                .collect(Collectors.groupingBy(s -> s.getStatut().name(), Collectors.counting()));
        stats.setParStatut(parStatut);

        log.info("Stats calculees: total={}, actives={}, levees={}, annulees={}",
                stats.getTotal(), stats.getActives(), stats.getLevees(), stats.getAnnulees());
        return stats;
    }

    private DashboardSanctionStats emptyStats() {
        DashboardSanctionStats stats = new DashboardSanctionStats();
        stats.setTotal(0);
        stats.setActives(0);
        stats.setLevees(0);
        stats.setAnnulees(0);
        stats.setParType(new HashMap<>());
        stats.setParStatut(new HashMap<>());
        return stats;
    }

    // ============================================
    // HELPER (existant)
    // ============================================

    private Employee getOrCreateEmployee(User user) {
        String userId = user.getId();

        Employee employee = employeeRepository.findByUserId(userId).orElse(null);
        if (employee != null) {
            log.info("Employe trouve par userId: {} - {}", employee.getId(), employee.getNomComplet());
            return employee;
        }

        String fullName = user.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] nameParts = fullName.trim().split(" ");
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)) : "";

            employee = employeeRepository.findAll()
                    .stream()
                    .filter(e -> {
                        boolean matchPrenom = firstName.equalsIgnoreCase(e.getPrenom() != null ? e.getPrenom() : "");
                        boolean matchNom = lastName.equalsIgnoreCase(e.getNom() != null ? e.getNom() : "");
                        return matchPrenom && matchNom;
                    })
                    .findFirst()
                    .orElse(null);

            if (employee != null) {
                log.info("Employe trouve par nom: {} - {}", employee.getId(), employee.getNomComplet());
                employee.setUser(user);
                employee.setUpdatedBy(userId);
                employee.setUpdatedAt(LocalDate.now());
                return employeeRepository.save(employee);
            }
        }

        log.info("Creation d'un nouvel employe pour l'utilisateur: {}", userId);
        Employee newEmployee = new Employee();
        newEmployee.setPrenom(user.getFirstName() != null ? user.getFirstName() : "Nouveau");
        newEmployee.setNom(user.getLastName() != null ? user.getLastName() : "Utilisateur");
        newEmployee.setMatriculeInterne("EMP-" + System.currentTimeMillis());
        newEmployee.setStatut("ACTIF");
        newEmployee.setUser(user);
        newEmployee.setCreatedAt(LocalDate.now());
        newEmployee.setCreatedBy(userId);

        if (user.getEmployeeId() != null) {
            newEmployee.setId(user.getEmployeeId());
        }

        return employeeRepository.save(newEmployee);
    }

    // ============================================
    // METHODES EXISTANTES (CREATE, UPDATE, DELETE, etc.)
    // ============================================

    public Page<SanctionDTO> getAllSanctions(Pageable pageable) {
        return sanctionRepository.findAll(pageable).map(sanctionMapper::toDTO);
    }

    public Page<SanctionDTO> getSanctionsByEmployee(String employeeId, Pageable pageable) {
        return sanctionRepository.findByEmploye_Id(employeeId, pageable).map(sanctionMapper::toDTO);
    }

    public SanctionDTO getSanctionById(String id) {
        Sanction sanction = sanctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sanction non trouvee avec l'id: " + id));
        return sanctionMapper.toDTO(sanction);
    }

    @Transactional
    public SanctionDTO createSanction(SanctionDTO dto, String userId) {
        Employee employe = employeeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new RuntimeException("Employe non trouve"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        DemandeExplication demande = null;
        if (dto.getDemandeExplicationId() != null) {
            demande = demandeRepository.findById(dto.getDemandeExplicationId())
                    .orElseThrow(() -> new RuntimeException("Demande d'explication non trouvee"));
        }

        String numero = generateNumeroSanction();
        while (sanctionRepository.existsByNumero(numero)) {
            numero = generateNumeroSanction();
        }

        Sanction sanction = new Sanction();
        sanction.setNumero(numero);
        sanction.setEmploye(employe);
        sanction.setDemandeExplication(demande);
        sanction.setType(dto.getType());
        sanction.setMotif(dto.getMotif());
        sanction.setDescription(dto.getDescription());
        sanction.setDateDebut(dto.getDateDebut());
        sanction.setDateFin(dto.getDateFin());
        sanction.setDuree(dto.getDuree());
        sanction.setStatut(StatutSanction.ACTIVE);
        sanction.setCreePar(user);
        sanction.setCreatedBy(userId);
        sanction.setCreatedAt(LocalDateTime.now());

        String userFullName = user.getFullName();
        if (userFullName.trim().isEmpty()) {
            userFullName = user.getEmail() != null ? user.getEmail() : userId;
        }

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(userFullName)
                .action(TypeActionHistorique.SANCTION_CREEE)
                .date(LocalDateTime.now())
                .commentaire("Sanction creee: " + dto.getMotif())
                .build();
        sanction.getHistorique().add(historique);

        Sanction saved = sanctionRepository.save(sanction);
        log.info("Sanction creee avec le numero: {}", saved.getNumero());

        return sanctionMapper.toDTO(saved);
    }

    @Transactional
    public SanctionDTO updateSanction(String id, SanctionDTO dto, String userId) {
        Sanction existing = sanctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sanction non trouvee"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        existing.setType(dto.getType());
        existing.setMotif(dto.getMotif());
        existing.setDescription(dto.getDescription());
        existing.setDateDebut(dto.getDateDebut());
        existing.setDateFin(dto.getDateFin());
        existing.setDuree(dto.getDuree());
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());

        String userFullName = user.getFullName();
        if (userFullName.trim().isEmpty()) {
            userFullName = user.getEmail() != null ? user.getEmail() : userId;
        }

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(userFullName)
                .action(TypeActionHistorique.SANCTION_MODIFIEE)
                .date(LocalDateTime.now())
                .commentaire("Sanction modifiee")
                .build();
        existing.getHistorique().add(historique);

        Sanction updated = sanctionRepository.save(existing);
        return sanctionMapper.toDTO(updated);
    }

    @Transactional
    public SanctionDTO leverSanction(String id, String userId) {
        Sanction sanction = sanctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sanction non trouvee"));

        if (sanction.getStatut() != StatutSanction.ACTIVE) {
            throw new RuntimeException("Seules les sanctions actives peuvent etre levees");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        sanction.setStatut(StatutSanction.ANNULEE);
        sanction.setUpdatedBy(userId);
        sanction.setUpdatedAt(LocalDateTime.now());

        String userFullName = user.getFullName();
        if (userFullName.trim().isEmpty()) {
            userFullName = user.getEmail() != null ? user.getEmail() : userId;
        }

        HistoriqueDiscipline historique = HistoriqueDiscipline.builder()
                .utilisateurId(userId)
                .utilisateurNom(userFullName)
                .action(TypeActionHistorique.SANCTION_LEVEE)
                .date(LocalDateTime.now())
                .commentaire("Sanction levee")
                .build();
        sanction.getHistorique().add(historique);

        Sanction updated = sanctionRepository.save(sanction);
        log.info("Sanction levee: {}", sanction.getNumero());

        return sanctionMapper.toDTO(updated);
    }

    @Transactional
    public void deleteSanction(String id) {
        sanctionRepository.deleteById(id);
        log.info("Sanction supprimee: {}", id);
    }

    private String generateNumeroSanction() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = sanctionRepository.count() + 1;
        return String.format("SAN-%s-%04d", year, count);
    }
}