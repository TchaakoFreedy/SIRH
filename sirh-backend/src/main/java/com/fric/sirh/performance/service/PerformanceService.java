package com.fric.sirh.performance.service;

import com.fric.sirh.performance.dto.*;
import com.fric.sirh.performance.enums.MentionPerformance;
import com.fric.sirh.performance.enums.PeriodeEvaluation;
import com.fric.sirh.performance.model.CriterePerformance;
import com.fric.sirh.performance.model.EvaluationPerformance;
import com.fric.sirh.performance.model.NoteEvaluation;
import com.fric.sirh.performance.repository.CriterePerformanceRepository;
import com.fric.sirh.performance.repository.EvaluationPerformanceRepository;
import com.fric.sirh.performance.mapper.CritereMapper;
import com.fric.sirh.performance.mapper.EvaluationMapper;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import com.fric.sirh.security.CurrentUserHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PerformanceService {

    private final EvaluationPerformanceRepository evaluationRepository;
    private final CriterePerformanceRepository critereRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EvaluationMapper evaluationMapper;
    private final CritereMapper critereMapper;
    private final CurrentUserHelper currentUserHelper;

    public PerformanceService(EvaluationPerformanceRepository evaluationRepository,
                              CriterePerformanceRepository critereRepository,
                              EmployeeRepository employeeRepository,
                              UserRepository userRepository,
                              EvaluationMapper evaluationMapper,
                              CritereMapper critereMapper,
                              CurrentUserHelper currentUserHelper) {
        this.evaluationRepository = evaluationRepository;
        this.critereRepository = critereRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.evaluationMapper = evaluationMapper;
        this.critereMapper = critereMapper;
        this.currentUserHelper = currentUserHelper;
    }

    // ==================== HELPER METHODS ====================

    private Employee getEmployeeByUserId(String userId) {
        log.info("Recherche d'employé pour l'utilisateur: {}", userId);

        if (userId == null || userId.isEmpty()) {
            log.warn("userId null ou vide");
            return null;
        }

        User user = null;

        // Si userId est un email, on récupère l'utilisateur via findByEmail
        if (userId.contains("@")) {
            Optional<User> userOpt = userRepository.findByEmail(userId);
            if (userOpt.isPresent()) {
                user = userOpt.get();
                log.info("Utilisateur trouvé par email: {}", user.getEmail());
            } else {
                log.warn("Aucun utilisateur trouvé pour l'email: {}", userId);
                return null;
            }
        } else {
            // Sinon, on considère que c'est un ID
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                log.warn("Aucun utilisateur trouvé pour l'ID: {}", userId);
                return null;
            }
        }

        if (user == null) {
            return null;
        }

        // Vérifier si l'utilisateur a un employeeId
        if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
            Optional<Employee> empOpt = employeeRepository.findById(user.getEmployeeId());
            if (empOpt.isPresent()) {
                log.info("Employé trouvé via employeeId de l'utilisateur: {}", empOpt.get().getNomComplet());
                return empOpt.get();
            }
        }

        // Essayer de trouver l'employé par userId (champ user.id)
        Optional<Employee> empByUser = employeeRepository.findByUserId(user.getId());
        if (empByUser.isPresent()) {
            log.info("Employé trouvé par findByUserId: {}", empByUser.get().getNomComplet());
            return empByUser.get();
        }

        // Essayer de trouver par email via la méthode existante
        Optional<Employee> empByEmail = employeeRepository.findByUserEmail(user.getEmail());
        if (empByEmail.isPresent()) {
            log.info("Employé trouvé via email: {}", empByEmail.get().getNomComplet());
            return empByEmail.get();
        }

        log.warn("Aucun employé trouvé pour l'utilisateur: {}", userId);
        return null;
    }

    private String getNomMois(int mois) {
        String[] moisNoms = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return moisNoms[mois - 1];
    }

    private Integer getNumeroMois(String mois) {
        Map<String, Integer> moisMap = new HashMap<>();
        moisMap.put("Janvier", 1);
        moisMap.put("Février", 2);
        moisMap.put("Mars", 3);
        moisMap.put("Avril", 4);
        moisMap.put("Mai", 5);
        moisMap.put("Juin", 6);
        moisMap.put("Juillet", 7);
        moisMap.put("Août", 8);
        moisMap.put("Septembre", 9);
        moisMap.put("Octobre", 10);
        moisMap.put("Novembre", 11);
        moisMap.put("Décembre", 12);
        return moisMap.getOrDefault(mois, 1);
    }

    private String getPeriodeDisplay(PeriodeEvaluation periode, Integer annee, Integer mois) {
        if (periode == null) return "Période non définie";

        if (periode.isMensuel() && mois != null) {
            return getNomMois(mois) + " " + annee;
        }

        return periode.getLibelle() + " " + annee;
    }

    // ==================== CRITÈRES ====================

    public List<CriterePerformanceDTO> getAllCriteres() {
        return critereRepository.findAll().stream()
                .map(this::ensureDefaultValues)
                .sorted(Comparator.comparing(CriterePerformance::getOrdreAffichage, Comparator.nullsLast(Integer::compareTo)))
                .map(critereMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<CriterePerformanceDTO> getActiveCriteres() {
        return critereRepository.findByActifTrue().stream()
                .map(this::ensureDefaultValues)
                .sorted(Comparator.comparing(CriterePerformance::getOrdreAffichage, Comparator.nullsLast(Integer::compareTo)))
                .map(critereMapper::toDTO)
                .collect(Collectors.toList());
    }

    public CriterePerformanceDTO getCritereById(String id) {
        CriterePerformance critere = critereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Critère non trouvé"));
        return critereMapper.toDTO(ensureDefaultValues(critere));
    }

    @Transactional
    public CriterePerformanceDTO createCritere(CriterePerformanceDTO dto, String userId) {
        CriterePerformance critere = critereMapper.toEntity(dto);

        if (critere.getTypeCritere() == null) {
            critere.setTypeCritere("GLOBAL");
        }
        if (critere.getActif() == null) {
            critere.setActif(true);
        }
        if (critere.getEmployeeIds() == null) {
            critere.setEmployeeIds(new ArrayList<>());
        }
        if (critere.getDepartementIds() == null) {
            critere.setDepartementIds(new ArrayList<>());
        }
        if (critere.getOrdreAffichage() == null) {
            critere.setOrdreAffichage(0);
        }

        critere.setCreatedBy(userId);
        critere.setCreatedAt(LocalDateTime.now());

        CriterePerformance saved = critereRepository.save(critere);
        log.info("Critère de performance créé par l'utilisateur: {} - Type: {}", userId, critere.getTypeCritere());
        return critereMapper.toDTO(saved);
    }

    @Transactional
    public CriterePerformanceDTO updateCritere(String id, CriterePerformanceDTO dto, String userId) {
        CriterePerformance existing = critereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Critère non trouvé"));

        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setNoteMaximale(dto.getNoteMaximale());
        existing.setCoefficient(dto.getCoefficient());
        existing.setActif(dto.getActif());
        existing.setTypeCritere(dto.getTypeCritere());
        existing.setEmployeeIds(dto.getEmployeeIds() != null ? dto.getEmployeeIds() : new ArrayList<>());
        existing.setDepartementIds(dto.getDepartementIds() != null ? dto.getDepartementIds() : new ArrayList<>());
        existing.setOrdreAffichage(dto.getOrdreAffichage() != null ? dto.getOrdreAffichage() : 0);
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());

        CriterePerformance updated = critereRepository.save(existing);
        log.info("Critère de performance mis à jour par l'utilisateur: {}", userId);
        return critereMapper.toDTO(updated);
    }

    @Transactional
    public void deleteCritere(String id) {
        critereRepository.deleteById(id);
        log.info("Critère de performance supprimé: {}", id);
    }

    private CriterePerformance ensureDefaultValues(CriterePerformance critere) {
        if (critere == null) {
            return null;
        }

        if (critere.getTypeCritere() == null) {
            critere.setTypeCritere("GLOBAL");
            log.debug("typeCritere défini à 'GLOBAL' pour le critère: {}", critere.getNom());
        }

        if (critere.getEmployeeIds() == null) {
            critere.setEmployeeIds(new ArrayList<>());
        }

        if (critere.getDepartementIds() == null) {
            critere.setDepartementIds(new ArrayList<>());
        }

        if (critere.getOrdreAffichage() == null) {
            critere.setOrdreAffichage(0);
        }

        if (critere.getActif() == null) {
            critere.setActif(true);
        }

        return critere;
    }

    // ==================== CRITÈRES PAR EMPLOYÉ ====================

    public List<CriterePerformance> getCriteresForEmployee(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeeId));

        List<CriterePerformance> criteres = new ArrayList<>();

        List<CriterePerformance> allActive = critereRepository.findByActifTrue();
        List<CriterePerformance> globaux = allActive.stream()
                .filter(c -> {
                    String type = c.getTypeCritere();
                    return type == null || "GLOBAL".equals(type);
                })
                .map(this::ensureDefaultValues)
                .collect(Collectors.toList());
        criteres.addAll(globaux);

        List<CriterePerformance> selectifs = critereRepository.findByActifTrueAndTypeCritereAndEmployeeIdsContaining("SELECTIVE", employeeId)
                .stream()
                .map(this::ensureDefaultValues)
                .collect(Collectors.toList());
        criteres.addAll(selectifs);

        List<CriterePerformance> additionalSelectifs = allActive.stream()
                .filter(c -> "SELECTIVE".equals(c.getTypeCritere()))
                .filter(c -> c.getEmployeeIds() != null && c.getEmployeeIds().contains(employeeId))
                .map(this::ensureDefaultValues)
                .collect(Collectors.toList());
        criteres.addAll(additionalSelectifs);

        Set<String> uniqueIds = new HashSet<>();
        List<CriterePerformance> uniqueCriteres = new ArrayList<>();
        for (CriterePerformance c : criteres) {
            if (uniqueIds.add(c.getId())) {
                uniqueCriteres.add(c);
            }
        }

        uniqueCriteres.sort(Comparator.comparing(CriterePerformance::getOrdreAffichage, Comparator.nullsLast(Integer::compareTo)));

        log.info("✅ {} critères trouvés pour l'employé {}: {} globaux, {} selectifs",
                uniqueCriteres.size(), employeeId,
                globaux.size(), selectifs.size());

        return uniqueCriteres;
    }

    public Map<String, List<CriterePerformance>> getCriteresForEmployees(List<String> employeeIds) {
        Map<String, List<CriterePerformance>> result = new HashMap<>();

        List<CriterePerformance> allActive = critereRepository.findByActifTrue();

        List<CriterePerformance> globaux = allActive.stream()
                .filter(c -> {
                    String type = c.getTypeCritere();
                    return type == null || "GLOBAL".equals(type);
                })
                .map(this::ensureDefaultValues)
                .collect(Collectors.toList());

        List<CriterePerformance> allSelectifs = allActive.stream()
                .filter(c -> "SELECTIVE".equals(c.getTypeCritere()))
                .map(this::ensureDefaultValues)
                .collect(Collectors.toList());

        for (String employeeId : employeeIds) {
            List<CriterePerformance> criteres = new ArrayList<>(globaux);

            for (CriterePerformance c : allSelectifs) {
                if (c.getEmployeeIds() != null && c.getEmployeeIds().contains(employeeId)) {
                    criteres.add(c);
                }
            }

            criteres.sort(Comparator.comparing(CriterePerformance::getOrdreAffichage, Comparator.nullsLast(Integer::compareTo)));
            result.put(employeeId, criteres);
        }

        return result;
    }

    public List<CriterePerformance> getCriteresByType(String type) {
        if (type == null || type.isEmpty()) {
            return new ArrayList<>();
        }

        List<CriterePerformance> result;
        if ("GLOBAL".equals(type)) {
            result = critereRepository.findByActifTrue().stream()
                    .filter(c -> c.getTypeCritere() == null || "GLOBAL".equals(c.getTypeCritere()))
                    .map(this::ensureDefaultValues)
                    .collect(Collectors.toList());
        } else {
            result = critereRepository.findByActifTrueAndTypeCritere(type)
                    .stream()
                    .map(this::ensureDefaultValues)
                    .collect(Collectors.toList());
        }

        return result;
    }

    // ==================== FILTER CRITERES FOR EVALUATION ====================

    private List<CriterePerformance> getFilteredCriteresForEmployee(String employeeId, List<String> critereIds) {
        List<CriterePerformance> allApplicable = getCriteresForEmployee(employeeId);
        if (critereIds == null || critereIds.isEmpty()) {
            return allApplicable;
        }

        // Filtrer pour ne garder que ceux dont l'ID est dans la liste fournie
        Set<String> requestedIds = new HashSet<>(critereIds);
        List<CriterePerformance> filtered = allApplicable.stream()
                .filter(c -> requestedIds.contains(c.getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new RuntimeException("Aucun des critères demandés n'est applicable à cet employé.");
        }

        // Vérifier que tous les IDs demandés existent dans les critères applicables
        Set<String> availableIds = allApplicable.stream().map(CriterePerformance::getId).collect(Collectors.toSet());
        for (String id : critereIds) {
            if (!availableIds.contains(id)) {
                log.warn("Le critère avec l'ID {} n'est pas applicable à l'employé {}", id, employeeId);
                // On peut soit ignorer, soit lever une erreur. Ici on ignore silencieusement.
            }
        }

        return filtered;
    }

    // ==================== ÉVALUATIONS ====================

    private Pageable correctSortForEvaluations(Pageable pageable) {
        if (pageable.getSort() == null || !pageable.getSort().isSorted()) {
            return pageable;
        }

        List<Sort.Order> correctedOrders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            Sort.Direction direction = order.getDirection();

            switch (property) {
                case "employeNom":
                    property = "employe.nom";
                    break;
                case "employePrenom":
                    property = "employe.prenom";
                    break;
                case "employeId":
                    property = "employe.id";
                    break;
                case "dateEvaluation":
                    property = "dateEvaluation";
                    break;
                case "createdAt":
                    property = "createdAt";
                    break;
                case "periode":
                    property = "periode";
                    break;
                case "annee":
                    property = "annee";
                    break;
                case "mention":
                    property = "mention";
                    break;
                case "pourcentage":
                    property = "pourcentage";
                    break;
                default:
                    break;
            }

            correctedOrders.add(new Sort.Order(direction, property));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(correctedOrders));
    }

    public Page<EvaluationPerformanceDTO> getAllEvaluations(Pageable pageable) {
        Pageable correctedPageable = correctSortForEvaluations(pageable);
        return evaluationRepository.findAll(correctedPageable).map(evaluationMapper::toDTO);
    }

    public Page<EvaluationPerformanceDTO> getEvaluationsByEmployee(String employeeId, Pageable pageable) {
        return evaluationRepository.findByEmployeId(employeeId, pageable).map(evaluationMapper::toDTO);
    }

    public EvaluationPerformanceDTO getEvaluationById(String id) {
        EvaluationPerformance evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        return evaluationMapper.toDTO(evaluation);
    }

    public Page<EvaluationPerformanceDTO> getMyEvaluations(String userId, Pageable pageable) {
        Employee employee = getEmployeeByUserId(userId);

        if (employee == null) {
            log.warn("Aucun employé trouvé pour l'utilisateur: {}. Retourne une page vide.", userId);
            return Page.empty(pageable);
        }

        return evaluationRepository.findByEmployeId(employee.getId(), pageable)
                .map(evaluationMapper::toDTO);
    }

    public PerformanceStatsDTO getMyPerformanceStats(String userId) {
        Employee employee = getEmployeeByUserId(userId);

        if (employee == null) {
            log.warn("Aucun employé trouvé pour l'utilisateur: {}. Retourne des stats vides.", userId);
            return PerformanceStatsDTO.builder()
                    .employeeId(null)
                    .employeeName("Utilisateur sans employé")
                    .employeePoste("Non défini")
                    .departement(null)
                    .totalEvaluations(0L)
                    .moyenneGenerale(0.0)
                    .meilleureMention(null)
                    .derniereMention(null)
                    .repartitionMentions(new ArrayList<>())
                    .evolution(new ArrayList<>())
                    .build();
        }

        List<EvaluationPerformance> evaluations = evaluationRepository.findByEmployeId(employee.getId());

        if (evaluations.isEmpty()) {
            return PerformanceStatsDTO.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getNomComplet())
                    .employeePoste(getEmployeePoste(employee))
                    .departement(employee.getDepartementId())
                    .totalEvaluations(0L)
                    .moyenneGenerale(0.0)
                    .meilleureMention(null)
                    .derniereMention(null)
                    .repartitionMentions(new ArrayList<>())
                    .evolution(new ArrayList<>())
                    .build();
        }

        double moyenneGenerale = evaluations.stream()
                .mapToDouble(EvaluationPerformance::getPourcentage)
                .average()
                .orElse(0.0);

        EvaluationPerformance meilleure = evaluations.stream()
                .max(Comparator.comparing(EvaluationPerformance::getPourcentage))
                .orElse(null);

        EvaluationPerformance derniere = evaluations.stream()
                .max(Comparator.comparing(EvaluationPerformance::getDateEvaluation))
                .orElse(null);

        Map<String, Long> mentionCount = evaluations.stream()
                .filter(e -> e.getMention() != null)
                .collect(Collectors.groupingBy(
                        EvaluationPerformance::getMention,
                        Collectors.counting()
                ));

        List<MentionDistributionDTO> repartitionMentions = new ArrayList<>();
        long total = evaluations.size();
        for (Map.Entry<String, Long> entry : mentionCount.entrySet()) {
            double pourcentage = (entry.getValue().doubleValue() / total) * 100;
            repartitionMentions.add(MentionDistributionDTO.builder()
                    .mention(entry.getKey())
                    .count(entry.getValue().intValue())
                    .pourcentage(pourcentage)
                    .build());
        }

        List<EvolutionPerformanceDTO> evolution = getEvolutionForEmployee(evaluations);

        return PerformanceStatsDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getNomComplet())
                .employeePoste(getEmployeePoste(employee))
                .departement(employee.getDepartementId())
                .totalEvaluations((long) evaluations.size())
                .moyenneGenerale(moyenneGenerale)
                .meilleureMention(meilleure != null ? meilleure.getMention() : null)
                .derniereMention(derniere != null ? derniere.getMention() : null)
                .repartitionMentions(repartitionMentions)
                .evolution(evolution)
                .build();
    }

    private String getEmployeePoste(Employee employee) {
        if (employee == null) {
            return "Poste non spécifié";
        }
        String posteId = employee.getPosteId();
        return posteId != null && !posteId.isEmpty() ? posteId : "Poste non spécifié";
    }

    public List<EvolutionPerformanceDTO> getMyPerformanceEvolution(String userId) {
        Employee employee = getEmployeeByUserId(userId);

        if (employee == null) {
            log.warn("Aucun employé trouvé pour l'utilisateur: {}. Retourne une liste vide.", userId);
            return new ArrayList<>();
        }

        List<EvaluationPerformance> evaluations = evaluationRepository.findByEmployeId(employee.getId());

        if (evaluations.isEmpty()) {
            return new ArrayList<>();
        }

        return getEvolutionForEmployee(evaluations);
    }

    private List<EvolutionPerformanceDTO> getEvolutionForEmployee(List<EvaluationPerformance> evaluations) {
        Map<String, List<EvaluationPerformance>> byPeriod = evaluations.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getPeriode() + "|" + e.getAnnee() + "|" + (e.getMois() != null ? e.getMois() : "")
                ));

        List<EvolutionPerformanceDTO> evolution = new ArrayList<>();
        for (Map.Entry<String, List<EvaluationPerformance>> entry : byPeriod.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String periode = parts[0];
            int annee = Integer.parseInt(parts[1]);
            Integer mois = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : null;

            double moyenne = entry.getValue().stream()
                    .mapToDouble(EvaluationPerformance::getPourcentage)
                    .average()
                    .orElse(0.0);

            String label;
            if (mois != null) {
                label = getNomMois(mois) + " " + annee;
            } else {
                try {
                    PeriodeEvaluation periodeEnum = PeriodeEvaluation.valueOf(periode);
                    label = periodeEnum.getLibelle() + " " + annee;
                } catch (IllegalArgumentException e) {
                    label = periode + " " + annee;
                }
            }

            evolution.add(EvolutionPerformanceDTO.builder()
                    .mois(label)
                    .annee(annee)
                    .moyenne(moyenne)
                    .nombreEvaluations((long) entry.getValue().size())
                    .build());
        }

        evolution.sort((e1, e2) -> {
            if (e1.getAnnee().equals(e2.getAnnee())) {
                return getNumeroMois(e1.getMois()).compareTo(getNumeroMois(e2.getMois()));
            }
            return e1.getAnnee().compareTo(e2.getAnnee());
        });

        if (evolution.size() > 12) {
            evolution = evolution.subList(evolution.size() - 12, evolution.size());
        }

        return evolution;
    }

    @Transactional
    public EvaluationPerformanceDTO createEvaluation(EvaluationPerformanceDTO dto, String userId) {
        log.info("Création d'une évaluation pour l'employé: {}, période: {}, année: {}",
                dto.getEmployeId(), dto.getPeriode(), dto.getAnnee());

        Employee employe = employeeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        User evaluateur = currentUserHelper.getCurrentUser();
        if (evaluateur == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        String evaluatorId = evaluateur.getId();

        PeriodeEvaluation periode = dto.getPeriode();
        EvaluationPerformance existing = null;

        if (periode.isMensuel() && dto.getMois() != null) {
            existing = evaluationRepository.findByEmployeIdAndPeriodeAndAnneeAndMois(
                    dto.getEmployeId(), periode, dto.getAnnee(), dto.getMois());
        } else {
            existing = evaluationRepository.findByEmployeIdAndPeriodeAndAnnee(
                    dto.getEmployeId(), periode, dto.getAnnee());
        }

        if (existing != null) {
            String periodeDisplay = getPeriodeDisplay(periode, dto.getAnnee(), dto.getMois());
            throw new RuntimeException("Une évaluation existe déjà pour cet employé pour la période: " + periodeDisplay);
        }

        // Récupérer les critères applicables filtrés selon la demande
        List<CriterePerformance> criteresApplicables = getFilteredCriteresForEmployee(dto.getEmployeId(), dto.getCritereIds());

        if (criteresApplicables.isEmpty()) {
            throw new RuntimeException("Aucun critère trouvé pour l'évaluation de cet employé.");
        }

        // Construire une map note par critère à partir des notes fournies
        Map<String, Double> notesParCritere = new HashMap<>();
        if (dto.getNotes() != null) {
            for (NoteEvaluationDTO noteDTO : dto.getNotes()) {
                notesParCritere.put(noteDTO.getCritereId(), noteDTO.getNote());
            }
        }

        double totalObtenu = 0;
        double totalMaximal = 0;
        List<NoteEvaluation> notes = new ArrayList<>();
        List<String> criteresUtilises = new ArrayList<>();

        for (CriterePerformance critere : criteresApplicables) {
            String critereId = critere.getId();
            Double note = notesParCritere.get(critereId);

            if (note == null) {
                note = 0.0;
            }

            if (note > critere.getNoteMaximale()) {
                throw new RuntimeException("La note ne peut pas dépasser " + critere.getNoteMaximale() +
                        " pour le critère: " + critere.getNom());
            }

            double scorePondere = note * critere.getCoefficient();

            NoteEvaluation noteEval = NoteEvaluation.builder()
                    .critereId(critere.getId())
                    .critereNom(critere.getNom())
                    .note(note)
                    .coefficient(critere.getCoefficient())
                    .scorePondere(scorePondere)
                    .build();

            notes.add(noteEval);
            criteresUtilises.add(critereId);
            totalObtenu += scorePondere;
            totalMaximal += critere.getNoteMaximale() * critere.getCoefficient();
        }

        double pourcentage = totalMaximal > 0 ? (totalObtenu / totalMaximal) * 100 : 0;
        String mention = MentionPerformance.fromScore(pourcentage).getLibelle();

        // Déterminer le type d'évaluation
        String typeEvaluation = (dto.getCritereIds() != null && !dto.getCritereIds().isEmpty()) ? "INDIVIDUELLE" : "GLOBALE";

        EvaluationPerformance evaluation = EvaluationPerformance.builder()
                .employe(employe)
                .evaluateur(evaluateur)
                .periode(periode)
                .annee(dto.getAnnee())
                .mois(dto.getMois())
                .commentaires(dto.getCommentaires())
                .dateEvaluation(LocalDateTime.now())
                .notes(notes)
                .criteresUtilises(criteresUtilises)
                .typeEvaluation(typeEvaluation)
                .totalObtenu(totalObtenu)
                .totalMaximal(totalMaximal)
                .pourcentage(pourcentage)
                .mention(mention)
                .createdBy(evaluatorId)
                .createdAt(LocalDateTime.now())
                .build();

        EvaluationPerformance saved = evaluationRepository.save(evaluation);
        log.info("Évaluation créée pour l'employé: {} - {} critères utilisés - Mention: {}",
                employe.getNomComplet(), criteresUtilises.size(), mention);

        return evaluationMapper.toDTO(saved);
    }

    @Transactional
    public EvaluationPerformanceDTO updateEvaluation(String id, EvaluationPerformanceDTO dto, String userId) {
        EvaluationPerformance existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));

        if (dto.getPeriode() != null) {
            existing.setPeriode(dto.getPeriode());
        }

        if (dto.getAnnee() != null) {
            existing.setAnnee(dto.getAnnee());
        }

        existing.setMois(dto.getMois());

        if (dto.getCommentaires() != null) {
            existing.setCommentaires(dto.getCommentaires());
        }

        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) {
            // On recalcule avec les critères actuellement utilisés (stockés dans l'évaluation)
            List<String> currentCritereIds = existing.getCriteresUtilises();
            // Récupérer les critères correspondants
            List<CriterePerformance> criteres = new ArrayList<>();
            for (String critereId : currentCritereIds) {
                critereRepository.findById(critereId)
                        .ifPresent(criteres::add);
            }

            if (criteres.isEmpty()) {
                throw new RuntimeException("Les critères de cette évaluation n'existent plus.");
            }

            double totalObtenu = 0;
            double totalMaximal = 0;
            List<NoteEvaluation> updatedNotes = new ArrayList<>();

            // Construire une map note par critère
            Map<String, Double> notesMap = new HashMap<>();
            for (NoteEvaluationDTO noteDTO : dto.getNotes()) {
                notesMap.put(noteDTO.getCritereId(), noteDTO.getNote());
            }

            for (CriterePerformance critere : criteres) {
                Double note = notesMap.get(critere.getId());
                if (note == null) {
                    note = 0.0;
                }

                if (note > critere.getNoteMaximale()) {
                    throw new RuntimeException("La note ne peut pas dépasser " + critere.getNoteMaximale() +
                            " pour le critère: " + critere.getNom());
                }

                double scorePondere = note * critere.getCoefficient();

                NoteEvaluation noteEval = NoteEvaluation.builder()
                        .critereId(critere.getId())
                        .critereNom(critere.getNom())
                        .note(note)
                        .coefficient(critere.getCoefficient())
                        .scorePondere(scorePondere)
                        .build();

                updatedNotes.add(noteEval);
                totalObtenu += scorePondere;
                totalMaximal += critere.getNoteMaximale() * critere.getCoefficient();
            }

            double pourcentage = totalMaximal > 0 ? (totalObtenu / totalMaximal) * 100 : 0;
            String mention = MentionPerformance.fromScore(pourcentage).getLibelle();

            existing.setNotes(updatedNotes);
            existing.setTotalObtenu(totalObtenu);
            existing.setTotalMaximal(totalMaximal);
            existing.setPourcentage(pourcentage);
            existing.setMention(mention);
        }

        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());

        EvaluationPerformance updated = evaluationRepository.save(existing);
        log.info("Évaluation de performance mise à jour par l'utilisateur: {}", userId);
        return evaluationMapper.toDTO(updated);
    }

    @Transactional
    public void deleteEvaluation(String id) {
        evaluationRepository.deleteById(id);
        log.info("Évaluation de performance supprimée: {}", id);
    }

    // ==================== CLASSEMENT ====================

    public List<ClassementDTO> getClassement(Integer annee, String entrepriseId, String departementId, Integer top) {
        List<EvaluationPerformance> evaluations = evaluationRepository.findByAnnee(annee);

        // Filtrer les évaluations qui ont un employé non null
        evaluations = evaluations.stream()
                .filter(e -> e.getEmploye() != null)
                .collect(Collectors.toList());

        if (entrepriseId != null) {
            evaluations = evaluations.stream()
                    .filter(e -> e.getEmploye().getEntrepriseId() != null &&
                            e.getEmploye().getEntrepriseId().equals(entrepriseId))
                    .collect(Collectors.toList());
        }

        if (departementId != null) {
            evaluations = evaluations.stream()
                    .filter(e -> e.getEmploye().getDepartementId() != null &&
                            e.getEmploye().getDepartementId().equals(departementId))
                    .collect(Collectors.toList());
        }

        Map<String, List<EvaluationPerformance>> evaluationsByEmployee = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getEmploye().getId()));

        List<ClassementDTO> classement = new ArrayList<>();

        for (Map.Entry<String, List<EvaluationPerformance>> entry : evaluationsByEmployee.entrySet()) {
            String employeeId = entry.getKey();
            List<EvaluationPerformance> empEvaluations = entry.getValue();

            double scoreMoyen = empEvaluations.stream()
                    .mapToDouble(EvaluationPerformance::getTotalObtenu)
                    .average()
                    .orElse(0);

            double pourcentageMoyen = empEvaluations.stream()
                    .mapToDouble(EvaluationPerformance::getPourcentage)
                    .average()
                    .orElse(0);

            String mention = MentionPerformance.fromScore(pourcentageMoyen).getLibelle();

            Employee employe = empEvaluations.get(0).getEmploye();

            ClassementDTO dto = ClassementDTO.builder()
                    .employeId(employeeId)
                    .employeNom(employe.getNomComplet())
                    .entrepriseId(employe.getEntrepriseId())
                    .departementId(employe.getDepartementId())
                    .scoreTotal(scoreMoyen)
                    .annee(annee)
                    .mention(mention)
                    .build();
            classement.add(dto);
        }

        classement.sort((c1, c2) -> c2.getScoreTotal().compareTo(c1.getScoreTotal()));

        for (int i = 0; i < classement.size(); i++) {
            classement.get(i).setRang(i + 1);
        }

        if (top != null && top > 0) {
            classement = classement.stream().limit(top).collect(Collectors.toList());
        }

        return classement;
    }

    public List<ClassementDTO> getTopEmployes(Integer annee, Integer top) {
        return getClassement(annee, null, null, top);
    }

    public RankDTO getMyRank(String userId, Integer annee) {
        Employee employee = getEmployeeByUserId(userId);

        if (employee == null) {
            log.warn("Aucun employé trouvé pour l'utilisateur: {}. Retourne un classement vide.", userId);
            return RankDTO.builder()
                    .employeeId(null)
                    .employeeName("Utilisateur sans employé")
                    .rang(0)
                    .totalEmployes(0)
                    .scoreTotal(0.0)
                    .annee(annee)
                    .build();
        }

        List<ClassementDTO> classement = getClassement(annee, null, null, null);

        if (classement.isEmpty()) {
            return RankDTO.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getNomComplet())
                    .rang(0)
                    .totalEmployes(0)
                    .scoreTotal(0.0)
                    .annee(annee)
                    .build();
        }

        Optional<ClassementDTO> employeeRank = classement.stream()
                .filter(c -> c.getEmployeId().equals(employee.getId()))
                .findFirst();

        if (employeeRank.isEmpty()) {
            return RankDTO.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getNomComplet())
                    .rang(classement.size() + 1)
                    .totalEmployes(classement.size())
                    .scoreTotal(0.0)
                    .annee(annee)
                    .build();
        }

        ClassementDTO rank = employeeRank.get();

        return RankDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getNomComplet())
                .rang(rank.getRang())
                .totalEmployes(classement.size())
                .scoreTotal(rank.getScoreTotal())
                .annee(annee)
                .build();
    }

    // ==================== DASHBOARD ====================

    public DashboardPerformanceDTO getDashboard() {
        List<EvaluationPerformance> evaluations = evaluationRepository.findAll().stream()
                .filter(e -> e.getEmploye() != null)
                .collect(Collectors.toList());

        int currentYear = LocalDateTime.now().getYear();

        Long totalEvaluations = (long) evaluations.size();

        double moyenneGenerale = evaluations.stream()
                .mapToDouble(EvaluationPerformance::getPourcentage)
                .average()
                .orElse(0);

        ClassementDTO meilleurEmploye = null;
        List<ClassementDTO> classement = getClassement(currentYear, null, null, 1);
        if (!classement.isEmpty()) {
            meilleurEmploye = classement.get(0);
        }

        String meilleurDepartement = null;
        Map<String, Double> moyenneParDepartement = evaluations.stream()
                .filter(e -> e.getEmploye().getDepartementId() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getEmploye().getDepartementId(),
                        Collectors.averagingDouble(EvaluationPerformance::getPourcentage)
                ));

        if (!moyenneParDepartement.isEmpty()) {
            meilleurDepartement = moyenneParDepartement.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        Map<String, Long> repartitionMentions = evaluations.stream()
                .filter(e -> e.getMention() != null)
                .collect(Collectors.groupingBy(
                        EvaluationPerformance::getMention,
                        Collectors.counting()
                ));

        List<EvolutionPerformanceDTO> evolutionParMois = new ArrayList<>();
        Map<String, List<EvaluationPerformance>> evaluationsParMois = evaluations.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .collect(Collectors.groupingBy(e -> {
                    LocalDateTime date = e.getDateEvaluation();
                    return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                }));

        for (Map.Entry<String, List<EvaluationPerformance>> entry : evaluationsParMois.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int annee = Integer.parseInt(parts[0]);
            int mois = Integer.parseInt(parts[1]);
            List<EvaluationPerformance> evalsMois = entry.getValue();

            double moyenne = evalsMois.stream()
                    .mapToDouble(EvaluationPerformance::getPourcentage)
                    .average()
                    .orElse(0);

            EvolutionPerformanceDTO dto = EvolutionPerformanceDTO.builder()
                    .mois(getNomMois(mois))
                    .annee(annee)
                    .moyenne(moyenne)
                    .nombreEvaluations((long) evalsMois.size())
                    .build();
            evolutionParMois.add(dto);
        }

        evolutionParMois.sort((e1, e2) -> {
            if (e1.getAnnee().equals(e2.getAnnee())) {
                return getNumeroMois(e1.getMois()).compareTo(getNumeroMois(e2.getMois()));
            }
            return e1.getAnnee().compareTo(e2.getAnnee());
        });

        return DashboardPerformanceDTO.builder()
                .totalEvaluations(totalEvaluations)
                .moyenneGenerale(moyenneGenerale)
                .meilleurEmploye(meilleurEmploye)
                .meilleurDepartement(meilleurDepartement)
                .repartitionMentions(repartitionMentions)
                .evolutionParMois(evolutionParMois)
                .build();
    }

    // ==================== EMPLOYEES FOR SELECTION ====================

    public List<EmployeeSelectionDTO> getEmployeesForSelection() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .map(emp -> EmployeeSelectionDTO.builder()
                        .id(emp.getId())
                        .nom(emp.getNom())
                        .prenom(emp.getPrenom())
                        .matriculeInterne(emp.getMatriculeInterne())
                        .departementId(emp.getDepartementId())
                        .statut(emp.getStatut())
                        .selected(false)
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== ADDITIONAL METHODS ====================

    public boolean checkExistingEvaluation(String employeId, String periode, Integer annee) {
        PeriodeEvaluation periodeEnum;
        try {
            periodeEnum = PeriodeEvaluation.valueOf(periode);
        } catch (IllegalArgumentException e) {
            log.warn("Période invalide: {}", periode);
            return false;
        }

        EvaluationPerformance existing = evaluationRepository
                .findByEmployeIdAndPeriodeAndAnnee(employeId, periodeEnum, annee);
        return existing != null;
    }

    public Map<String, Integer> getEvaluationYearRange() {
        List<EvaluationPerformance> evaluations = evaluationRepository.findAll();

        if (evaluations.isEmpty()) {
            Map<String, Integer> range = new HashMap<>();
            int currentYear = LocalDateTime.now().getYear();
            range.put("minYear", currentYear - 5);
            range.put("maxYear", currentYear + 5);
            return range;
        }

        int minYear = evaluations.stream()
                .mapToInt(EvaluationPerformance::getAnnee)
                .min()
                .orElse(LocalDateTime.now().getYear() - 5);

        int maxYear = evaluations.stream()
                .mapToInt(EvaluationPerformance::getAnnee)
                .max()
                .orElse(LocalDateTime.now().getYear() + 5);

        Map<String, Integer> range = new HashMap<>();
        range.put("minYear", minYear);
        range.put("maxYear", maxYear);
        return range;
    }
}