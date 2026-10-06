// src/main/java/com/fric/sirh/cacao/service/AvanceFinanciereService.java

package com.fric.sirh.cacao.service;

import com.fric.sirh.cacao.dto.AvanceFinanciereDto;
import com.fric.sirh.cacao.model.Acheteur;
import com.fric.sirh.cacao.model.AvanceFinanciere;
import com.fric.sirh.cacao.repository.AcheteurRepository;
import com.fric.sirh.cacao.repository.AvanceFinanciereRepository;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.model.Employee;
import com.fric.sirh.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvanceFinanciereService {

    private final AvanceFinanciereRepository avanceRepository;
    private final AcheteurRepository acheteurRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<AvanceFinanciereDto> getAllAvances() {
        return avanceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvanceFinanciereDto> getAvancesByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des avances avec un ID d'acheteur null ou vide");
            return List.of();
        }
        return avanceRepository.findByAcheteurIdOrderByDateAvanceDesc(acheteurId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AvanceFinanciereDto createAvance(AvanceFinanciereDto dto) {
        if (dto.getAcheteurId() == null || dto.getAcheteurId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }

        Acheteur acheteur = acheteurRepository.findById(dto.getAcheteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", dto.getAcheteurId()));

        if (dto.getQuantiteKg() == null || dto.getQuantiteKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantite en kg doit etre positive");
        }

        if (dto.getPrixUnitaire() == null || dto.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit etre positif");
        }

        BigDecimal montantCalcule = dto.getQuantiteKg().multiply(dto.getPrixUnitaire());
        if (dto.getMontant() != null && dto.getMontant().compareTo(BigDecimal.ZERO) > 0) {
            if (dto.getMontant().compareTo(montantCalcule) != 0) {
                log.warn("Le montant saisi {} ne correspond pas au calcul {} x {}",
                        dto.getMontant(), dto.getQuantiteKg(), dto.getPrixUnitaire());
            }
        }

        AvanceFinanciere avance = AvanceFinanciere.builder()
                .acheteurId(dto.getAcheteurId())
                .quantiteKg(dto.getQuantiteKg())
                .prixUnitaire(dto.getPrixUnitaire())
                .montant(montantCalcule)
                .dateAvance(dto.getDateAvance() != null ? dto.getDateAvance() : LocalDateTime.now())
                .motif(dto.getMotif())
                .modePaiement(dto.getModePaiement())
                .referencePaiement(dto.getReferencePaiement())
                .build();

        AvanceFinanciere saved = avanceRepository.save(avance);
        log.info("Avance de {} creee pour l'acheteur {} ({} kg x {})",
                saved.getMontant(), saved.getAcheteurId(), saved.getQuantiteKg(), saved.getPrixUnitaire());
        return toDto(saved);
    }

    @Transactional
    public void deleteAvance(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'avance est requis");
        }
        AvanceFinanciere avance = findEntityById(id);
        avanceRepository.delete(avance);
        log.info("Avance {} supprimee", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalAvancesByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return avanceRepository.sumMontantByAcheteurId(acheteurId);
    }

    @Transactional(readOnly = true)
    public AvanceFinanciereDto findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'avance est requis");
        }
        return avanceRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("AvanceFinanciere", id));
    }

    @Transactional(readOnly = true)
    public AvanceFinanciere findEntityById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'avance est requis");
        }
        return avanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AvanceFinanciere", id));
    }

    private AvanceFinanciereDto toDto(AvanceFinanciere avance) {
        Acheteur acheteur = acheteurRepository.findById(avance.getAcheteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", avance.getAcheteurId()));

        Employee employee = null;
        String nomComplet = "Employe inconnu";

        try {
            employee = employeeService.getById(acheteur.getEmployeeId());
            if (employee != null) {
                nomComplet = employee.getNomComplet();
            }
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'employe pour l'acheteur {}: {}", acheteur.getId(), e.getMessage());
        }

        return AvanceFinanciereDto.builder()
                .id(avance.getId())
                .acheteurId(avance.getAcheteurId())
                .quantiteKg(avance.getQuantiteKg())
                .prixUnitaire(avance.getPrixUnitaire())
                .montant(avance.getMontant())
                .dateAvance(avance.getDateAvance())
                .motif(avance.getMotif())
                .modePaiement(avance.getModePaiement())
                .referencePaiement(avance.getReferencePaiement())
                .acheteurNomComplet(nomComplet)
                .build();
    }
}