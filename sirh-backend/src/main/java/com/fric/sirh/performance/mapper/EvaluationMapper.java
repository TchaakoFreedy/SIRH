package com.fric.sirh.performance.mapper;

import com.fric.sirh.performance.dto.EvaluationPerformanceDTO;
import com.fric.sirh.performance.dto.NoteEvaluationDTO;
import com.fric.sirh.performance.model.EvaluationPerformance;
import com.fric.sirh.performance.model.NoteEvaluation;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EvaluationMapper {

    public EvaluationPerformanceDTO toDTO(EvaluationPerformance entity) {
        if (entity == null) return null;

        return EvaluationPerformanceDTO.builder()
                .id(entity.getId())
                .employeId(entity.getEmploye() != null ? entity.getEmploye().getId() : null)
                .employeNom(entity.getEmploye() != null ? entity.getEmploye().getNomComplet() : null)
                .evaluateurId(entity.getEvaluateur() != null ? entity.getEvaluateur().getId() : null)
                .evaluateurNom(entity.getEvaluateur() != null ? entity.getEvaluateur().getFullName() : null)
                .periode(entity.getPeriode())
                .annee(entity.getAnnee())
                .commentaires(entity.getCommentaires())
                .dateEvaluation(entity.getDateEvaluation())
                .notes(entity.getNotes() != null ? entity.getNotes().stream()
                        .map(this::toNoteDTO)
                        .collect(Collectors.toList()) : null)
                .totalObtenu(entity.getTotalObtenu())
                .totalMaximal(entity.getTotalMaximal())
                .pourcentage(entity.getPourcentage())
                .mention(entity.getMention())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NoteEvaluationDTO toNoteDTO(NoteEvaluation note) {
        if (note == null) return null;

        return NoteEvaluationDTO.builder()
                .critereId(note.getCritereId())
                .critereNom(note.getCritereNom())
                .note(note.getNote())
                .coefficient(note.getCoefficient())
                .scorePondere(note.getScorePondere())
                .build();
    }
}