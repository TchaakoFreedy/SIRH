// src/main/java/com/fric/sirh/cacao/service/ReceptionCacaoService.java

package com.fric.sirh.cacao.service;

import com.fric.sirh.cacao.dto.ReceptionCacaoDto;
import com.fric.sirh.cacao.model.Acheteur;
import com.fric.sirh.cacao.model.ReceptionCacao;
import com.fric.sirh.cacao.model.Remboursement;
import com.fric.sirh.cacao.repository.AcheteurRepository;
import com.fric.sirh.cacao.repository.ReceptionCacaoRepository;
import com.fric.sirh.cacao.repository.RemboursementRepository;
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
public class ReceptionCacaoService {

    private final ReceptionCacaoRepository receptionRepository;
    private final AcheteurRepository acheteurRepository;
    private final EmployeeService employeeService;
    private final RemboursementRepository remboursementRepository;

    @Transactional(readOnly = true)
    public List<ReceptionCacaoDto> getAllReceptions() {
        return receptionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceptionCacaoDto> getReceptionsByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des receptions avec un ID d'acheteur null ou vide");
            return List.of();
        }
        return receptionRepository.findByAcheteurIdOrderByDateReceptionDesc(acheteurId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReceptionCacaoDto createReception(ReceptionCacaoDto dto) {
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

        BigDecimal quantiteNet = dto.getQuantiteKg();
        BigDecimal quantiteRefractee = dto.getQuantiteRefractee() != null ? dto.getQuantiteRefractee() : BigDecimal.ZERO;

        if (quantiteRefractee.compareTo(BigDecimal.ZERO) > 0) {
            if (quantiteRefractee.compareTo(dto.getQuantiteKg()) > 0) {
                throw new IllegalArgumentException("La quantite refractee ne peut pas depasser la quantite apportee");
            }
            quantiteNet = dto.getQuantiteKg().subtract(quantiteRefractee);
            if (dto.getMotifRefraction() == null || dto.getMotifRefraction().trim().isEmpty()) {
                throw new IllegalArgumentException("Le motif de la refraction est requis lorsque la quantite refractee est superieure a zero");
            }
        }

        BigDecimal valeurLivree = quantiteNet.multiply(dto.getPrixUnitaire());

        ReceptionCacao reception = ReceptionCacao.builder()
                .acheteurId(dto.getAcheteurId())
                .dateReception(dto.getDateReception() != null ? dto.getDateReception() : LocalDateTime.now())
                .quantiteKg(dto.getQuantiteKg())
                .quantiteRefractee(quantiteRefractee)
                .quantiteNet(quantiteNet)
                .motifRefraction(dto.getMotifRefraction())
                .prixUnitaire(dto.getPrixUnitaire())
                .valeurLivree(valeurLivree)
                .qualite(dto.getQualite())
                .observations(dto.getObservations())
                .numBonReception(dto.getNumBonReception())
                .montantRembourse(BigDecimal.ZERO)
                .build();

        ReceptionCacao saved = receptionRepository.save(reception);

        String logMessage = String.format("Reception de %s kg (net: %s kg) enregistree pour l'acheteur %s",
                saved.getQuantiteKg(), saved.getQuantiteNet(), saved.getAcheteurId());
        if (quantiteRefractee.compareTo(BigDecimal.ZERO) > 0) {
            logMessage += String.format(" avec refraction de %s kg (%s)", quantiteRefractee, dto.getMotifRefraction());
        }
        log.info(logMessage);

        return toDto(saved);
    }

    @Transactional
    public void deleteReception(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la reception est requis");
        }
        ReceptionCacao reception = findEntityById(id);

        if (reception.getRemboursementId() != null && !reception.getRemboursementId().trim().isEmpty()) {
            throw new IllegalArgumentException("Impossible de supprimer une reception ayant un remboursement associe");
        }

        receptionRepository.delete(reception);
        log.info("Reception {} supprimee", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalValeurLivreeByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return receptionRepository.sumValeurLivreeByAcheteurId(acheteurId);
    }

    @Transactional(readOnly = true)
    public ReceptionCacaoDto findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la reception est requis");
        }
        return receptionRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("ReceptionCacao", id));
    }

    @Transactional(readOnly = true)
    public ReceptionCacao findEntityById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la reception est requis");
        }
        return receptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReceptionCacao", id));
    }

    @Transactional(readOnly = true)
    public List<ReceptionCacaoDto> getReceptionsNonRembourseesByAcheteur(String acheteurId) {
        if (acheteurId == null || acheteurId.trim().isEmpty()) {
            log.warn("Tentative de recuperation des receptions non remboursees avec un ID d'acheteur null ou vide");
            return List.of();
        }
        return receptionRepository.findNonRembourseesByAcheteurId(acheteurId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ReceptionCacaoDto toDto(ReceptionCacao reception) {
        Acheteur acheteur = acheteurRepository.findById(reception.getAcheteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Acheteur", reception.getAcheteurId()));

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

        return ReceptionCacaoDto.builder()
                .id(reception.getId())
                .acheteurId(reception.getAcheteurId())
                .dateReception(reception.getDateReception())
                .quantiteKg(reception.getQuantiteKg())
                .quantiteRefractee(reception.getQuantiteRefractee())
                .quantiteNet(reception.getQuantiteNet())
                .motifRefraction(reception.getMotifRefraction())
                .prixUnitaire(reception.getPrixUnitaire())
                .valeurLivree(reception.getValeurLivree())
                .qualite(reception.getQualite())
                .observations(reception.getObservations())
                .numBonReception(reception.getNumBonReception())
                .montantRembourse(reception.getMontantRembourse())
                .remboursementId(reception.getRemboursementId())
                .acheteurNomComplet(nomComplet)
                .build();
    }
}