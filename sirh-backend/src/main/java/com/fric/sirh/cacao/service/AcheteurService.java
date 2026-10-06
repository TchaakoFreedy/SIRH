// src/main/java/com/fric/sirh/cacao/service/AcheteurService.java

package com.fric.sirh.cacao.service;

import com.fric.sirh.cacao.dto.AcheteurDto;
import com.fric.sirh.cacao.model.Acheteur;
import com.fric.sirh.cacao.repository.AcheteurRepository;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.Employee;
import com.fric.sirh.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcheteurService {

    private final AcheteurRepository acheteurRepository;
    private final EmployeeService employeeService;
    private final AvanceFinanciereService avanceService;
    private final ReceptionCacaoService receptionService;
    private final RemboursementService remboursementService;

    @Transactional(readOnly = true)
    public List<AcheteurDto> getAllAcheteursAvecSoldes() {
        List<Acheteur> acheteurs = acheteurRepository.findAllActifs();
        return acheteurs.stream()
                .map(this::toDtoWithSolde)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AcheteurDto> getAllAcheteursAvecSoldesIncluantSuspendus() {
        List<Acheteur> acheteurs = acheteurRepository.findAll();
        return acheteurs.stream()
                .map(this::toDtoWithSolde)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcheteurDto getAcheteurAvecSolde(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }
        Acheteur acheteur = findById(id);
        return toDtoWithSolde(acheteur);
    }

    @Transactional
    public AcheteurDto create(AcheteurDto dto) {
        if (dto.getEmployeeId() == null || dto.getEmployeeId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'employe est requis");
        }

        Employee employee = employeeService.getById(dto.getEmployeeId());
        if (employee == null) {
            throw new ResourceNotFoundException("Employee", dto.getEmployeeId());
        }

        Optional<Acheteur> existingAcheteur = acheteurRepository.findByEmployeeId(dto.getEmployeeId());

        if (existingAcheteur.isPresent()) {
            Acheteur existing = existingAcheteur.get();

            if ("ACTIF".equals(existing.getStatut())) {
                throw new BusinessException("Cet employe est deja enregistre comme acheteur actif");
            }

            if ("SUSPENDU".equals(existing.getStatut()) || "INACTIF".equals(existing.getStatut())) {
                log.info("Reactivation de l'acheteur suspendu pour l'employe {}", dto.getEmployeeId());
                existing.setStatut("ACTIF");
                if (dto.getZoneCollecte() != null && !dto.getZoneCollecte().trim().isEmpty()) {
                    existing.setZoneCollecte(dto.getZoneCollecte());
                }
                Acheteur reactivated = acheteurRepository.save(existing);
                log.info("Acheteur {} reactive pour l'employe {}", reactivated.getId(), dto.getEmployeeId());
                return toDtoWithSolde(reactivated);
            }
        }

        Acheteur acheteur = Acheteur.builder()
                .employeeId(dto.getEmployeeId())
                .zoneCollecte(dto.getZoneCollecte())
                .statut("ACTIF")
                .build();

        Acheteur saved = acheteurRepository.save(acheteur);
        log.info("Acheteur cree pour l'employe {}", dto.getEmployeeId());
        return toDtoWithSolde(saved);
    }

    @Transactional
    public AcheteurDto update(String id, AcheteurDto dto) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }
        Acheteur existing = findById(id);
        if (dto.getZoneCollecte() != null) {
            existing.setZoneCollecte(dto.getZoneCollecte());
        }
        if (dto.getStatut() != null) {
            existing.setStatut(dto.getStatut());
        }
        Acheteur updated = acheteurRepository.save(existing);
        log.info("Acheteur {} mis a jour", updated.getId());
        return toDtoWithSolde(updated);
    }

    @Transactional
    public void delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }
        Acheteur acheteur = findById(id);
        acheteur.setStatut("SUSPENDU");
        acheteurRepository.save(acheteur);
        log.info("Acheteur {} desactive", id);
    }

    @Transactional
    public AcheteurDto reactivate(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }
        Acheteur acheteur = findById(id);
        if ("ACTIF".equals(acheteur.getStatut())) {
            throw new BusinessException("Cet acheteur est deja actif");
        }
        acheteur.setStatut("ACTIF");
        Acheteur reactivated = acheteurRepository.save(acheteur);
        log.info("Acheteur {} reactive", id);
        return toDtoWithSolde(reactivated);
    }

    @Transactional(readOnly = true)
    public Acheteur findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }
        return acheteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", id));
    }

    @Transactional(readOnly = true)
    public AcheteurDto findByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'employe est requis");
        }
        Acheteur acheteur = acheteurRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", "employeeId", employeeId));
        return toDtoWithSolde(acheteur);
    }

    @Transactional(readOnly = true)
    public Optional<Acheteur> findExistingByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return Optional.empty();
        }
        return acheteurRepository.findByEmployeeId(employeeId);
    }

    private AcheteurDto toDtoWithSolde(Acheteur acheteur) {
        Employee employee = null;
        String nomComplet = "Employe inconnu";
        String telephone = "N/A";
        String email = "N/A";

        try {
            employee = employeeService.getById(acheteur.getEmployeeId());
            if (employee != null) {
                nomComplet = employee.getNomComplet();
                telephone = employee.getTelephone() != null ? employee.getTelephone() : "N/A";
                if (employee.getUser() != null && employee.getUser().getEmail() != null) {
                    email = employee.getUser().getEmail();
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'employe pour l'acheteur {}: {}", acheteur.getId(), e.getMessage());
        }

        BigDecimal totalAvances = avanceService.getTotalAvancesByAcheteur(acheteur.getId());
        BigDecimal totalValeurLivree = receptionService.getTotalValeurLivreeByAcheteur(acheteur.getId());
        BigDecimal totalRemboursements = remboursementService.getTotalRemboursementsByAcheteur(acheteur.getId());
        BigDecimal solde = totalAvances.subtract(totalValeurLivree).add(totalRemboursements);

        return AcheteurDto.builder()
                .id(acheteur.getId())
                .employeeId(acheteur.getEmployeeId())
                .zoneCollecte(acheteur.getZoneCollecte())
                .statut(acheteur.getStatut())
                .nomComplet(nomComplet)
                .telephone(telephone)
                .email(email)
                .totalAvances(totalAvances)
                .totalValeurLivree(totalValeurLivree)
                .solde(solde)
                .build();
    }
}