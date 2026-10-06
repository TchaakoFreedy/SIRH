// src/main/java/com/fric/sirh/cacao/service/RemboursementService.java

package com.fric.sirh.cacao.service;

import com.fric.sirh.cacao.dto.RemboursementDto;
import com.fric.sirh.cacao.model.Acheteur;
import com.fric.sirh.cacao.model.ReceptionCacao;
import com.fric.sirh.cacao.model.Remboursement;
import com.fric.sirh.cacao.repository.AcheteurRepository;
import com.fric.sirh.cacao.repository.ReceptionCacaoRepository;
import com.fric.sirh.cacao.repository.RemboursementRepository;
import com.fric.sirh.exception.BusinessException;
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
public class RemboursementService {

    private final RemboursementRepository remboursementRepository;
    private final ReceptionCacaoRepository receptionRepository;
    private final AcheteurRepository acheteurRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<RemboursementDto> getAllRemboursements() {
        return remboursementRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RemboursementDto> getRemboursementsByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des remboursements avec un ID d'acheteur null ou vide");
            return List.of();
        }
        return remboursementRepository.findByAcheteurIdOrderByDateRemboursementDesc(acheteurId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RemboursementDto createRemboursement(RemboursementDto dto) {
        if (dto.getAcheteurId() == null || dto.getAcheteurId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de l'acheteur est requis");
        }

        if (dto.getReceptionId() == null || dto.getReceptionId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la reception est requis");
        }

        Acheteur acheteur = acheteurRepository.findById(dto.getAcheteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", dto.getAcheteurId()));

        ReceptionCacao reception = receptionRepository.findById(dto.getReceptionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReceptionCacao", dto.getReceptionId()));

        if (!reception.getAcheteurId().equals(dto.getAcheteurId())) {
            throw new BusinessException("La reception n'appartient pas a cet acheteur");
        }

        if (reception.getRemboursementId() != null && !reception.getRemboursementId().trim().isEmpty()) {
            throw new BusinessException("Cette reception a deja fait l'objet d'un remboursement");
        }

        if (dto.getQuantiteAttendue() == null || dto.getQuantiteAttendue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantite attendue doit etre positive");
        }

        BigDecimal quantiteRecue = reception.getQuantiteNet() != null ? reception.getQuantiteNet() : reception.getQuantiteKg();
        if (quantiteRecue.compareTo(dto.getQuantiteAttendue()) <= 0) {
            throw new BusinessException("La quantite recue (" + quantiteRecue + " kg) n'est pas superieure a la quantite attendue (" + dto.getQuantiteAttendue() + " kg). Aucun remboursement necessaire.");
        }

        BigDecimal quantiteSurplus = quantiteRecue.subtract(dto.getQuantiteAttendue());

        if (dto.getPrixUnitaire() == null || dto.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit etre positif");
        }

        BigDecimal montantRembourse = quantiteSurplus.multiply(dto.getPrixUnitaire());

        if (dto.getMontantRembourse() != null && dto.getMontantRembourse().compareTo(BigDecimal.ZERO) > 0) {
            if (dto.getMontantRembourse().compareTo(montantRembourse) != 0) {
                log.warn("Le montant saisi {} ne correspond pas au calcul {} kg x {}",
                        dto.getMontantRembourse(), quantiteSurplus, dto.getPrixUnitaire());
            }
        }

        Remboursement remboursement = Remboursement.builder()
                .acheteurId(dto.getAcheteurId())
                .receptionId(dto.getReceptionId())
                .receptionNumBon(reception.getNumBonReception())
                .quantiteRecue(quantiteRecue)
                .quantiteAttendue(dto.getQuantiteAttendue())
                .quantiteSurplus(quantiteSurplus)
                .prixUnitaire(dto.getPrixUnitaire())
                .montantRembourse(montantRembourse)
                .motifRemboursement(dto.getMotifRemboursement())
                .modePaiement(dto.getModePaiement())
                .referencePaiement(dto.getReferencePaiement())
                .dateRemboursement(dto.getDateRemboursement() != null ? dto.getDateRemboursement() : LocalDateTime.now())
                .statut("EFFECTUE")
                .build();

        Remboursement saved = remboursementRepository.save(remboursement);

        reception.setMontantRembourse(montantRembourse);
        reception.setRemboursementId(saved.getId());
        receptionRepository.save(reception);

        log.info("Remboursement de {} cree pour la reception {} (surplus: {} kg)",
                saved.getMontantRembourse(), saved.getReceptionId(), saved.getQuantiteSurplus());

        return toDto(saved);
    }

    @Transactional
    public void deleteRemboursement(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID du remboursement est requis");
        }

        Remboursement remboursement = findEntityById(id);

        ReceptionCacao reception = receptionRepository.findById(remboursement.getReceptionId())
                .orElse(null);

        if (reception != null) {
            reception.setMontantRembourse(BigDecimal.ZERO);
            reception.setRemboursementId(null);
            receptionRepository.save(reception);
        }

        remboursementRepository.delete(remboursement);
        log.info("Remboursement {} supprime", id);
    }

    @Transactional(readOnly = true)
    public RemboursementDto findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID du remboursement est requis");
        }
        return remboursementRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Remboursement", id));
    }

    @Transactional(readOnly = true)
    public Remboursement findEntityById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID du remboursement est requis");
        }
        return remboursementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remboursement", id));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRemboursementsByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return remboursementRepository.sumMontantRembourseByAcheteurId(acheteurId);
    }

    private RemboursementDto toDto(Remboursement remboursement) {
        Acheteur acheteur = acheteurRepository.findById(remboursement.getAcheteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", remboursement.getAcheteurId()));

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

        return RemboursementDto.builder()
                .id(remboursement.getId())
                .acheteurId(remboursement.getAcheteurId())
                .acheteurNomComplet(nomComplet)
                .receptionId(remboursement.getReceptionId())
                .receptionNumBon(remboursement.getReceptionNumBon())
                .quantiteRecue(remboursement.getQuantiteRecue())
                .quantiteAttendue(remboursement.getQuantiteAttendue())
                .quantiteSurplus(remboursement.getQuantiteSurplus())
                .prixUnitaire(remboursement.getPrixUnitaire())
                .montantRembourse(remboursement.getMontantRembourse())
                .motifRemboursement(remboursement.getMotifRemboursement())
                .modePaiement(remboursement.getModePaiement())
                .referencePaiement(remboursement.getReferencePaiement())
                .dateRemboursement(remboursement.getDateRemboursement())
                .statut(remboursement.getStatut())
                .createdAt(remboursement.getCreatedAt())
                .updatedAt(remboursement.getUpdatedAt())
                .build();
    }
}