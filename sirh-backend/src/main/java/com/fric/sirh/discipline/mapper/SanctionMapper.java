package com.fric.sirh.discipline.mapper;

import com.fric.sirh.discipline.dto.SanctionDTO;
import com.fric.sirh.discipline.model.Sanction;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SanctionMapper {

    public SanctionDTO toDTO(Sanction entity) {
        if (entity == null) return null;

        SanctionDTO dto = new SanctionDTO();
        dto.setId(entity.getId());
        dto.setNumero(entity.getNumero());
        dto.setEmployeId(entity.getEmploye() != null ? entity.getEmploye().getId() : null);
        dto.setEmployeNom(entity.getEmploye() != null ? entity.getEmploye().getNomComplet() : null);
        dto.setDemandeExplicationId(entity.getDemandeExplication() != null ? entity.getDemandeExplication().getId() : null);
        dto.setDemandeExplicationNumero(entity.getDemandeExplication() != null ? entity.getDemandeExplication().getNumero() : null);
        dto.setType(entity.getType());
        dto.setMotif(entity.getMotif());
        dto.setDescription(entity.getDescription());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setDuree(entity.getDuree());
        dto.setStatut(entity.getStatut());
        dto.setCreeParId(entity.getCreePar() != null ? entity.getCreePar().getId() : null);
        dto.setCreeParNom(entity.getCreePar() != null ? entity.getCreePar().getFullName() : null);

        if (entity.getHistorique() != null) {
            dto.setHistorique(entity.getHistorique().stream()
                    .map(h -> {
                        com.fric.sirh.discipline.dto.HistoriqueDisciplineDTO hDto = new com.fric.sirh.discipline.dto.HistoriqueDisciplineDTO();
                        hDto.setUtilisateurId(h.getUtilisateurId());
                        hDto.setUtilisateurNom(h.getUtilisateurNom());
                        hDto.setAction(h.getAction());
                        hDto.setDate(h.getDate());
                        hDto.setCommentaire(h.getCommentaire());
                        return hDto;
                    })
                    .collect(Collectors.toList()));
        }

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }
}