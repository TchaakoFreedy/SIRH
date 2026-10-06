package com.fric.sirh.performance.mapper;

import com.fric.sirh.performance.dto.CriterePerformanceDTO;
import com.fric.sirh.performance.model.CriterePerformance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CritereMapper {

    public CriterePerformanceDTO toDTO(CriterePerformance entity) {
        if (entity == null) return null;

        return CriterePerformanceDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .noteMaximale(entity.getNoteMaximale())
                .coefficient(entity.getCoefficient())
                .actif(entity.getActif())
                .typeCritere(entity.getTypeCritere())
                .employeeIds(entity.getEmployeeIds())
                .departementIds(entity.getDepartementIds())
                .ordreAffichage(entity.getOrdreAffichage())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CriterePerformance toEntity(CriterePerformanceDTO dto) {
        if (dto == null) return null;

        return CriterePerformance.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .description(dto.getDescription())
                .noteMaximale(dto.getNoteMaximale())
                .coefficient(dto.getCoefficient())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .typeCritere(dto.getTypeCritere() != null ? dto.getTypeCritere() : "GLOBAL")
                .employeeIds(dto.getEmployeeIds() != null ? dto.getEmployeeIds() : new ArrayList<>())
                .departementIds(dto.getDepartementIds() != null ? dto.getDepartementIds() : new ArrayList<>())
                .ordreAffichage(dto.getOrdreAffichage() != null ? dto.getOrdreAffichage() : 0)
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedBy(dto.getUpdatedBy())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}