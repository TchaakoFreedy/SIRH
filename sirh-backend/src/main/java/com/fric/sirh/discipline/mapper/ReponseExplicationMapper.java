package com.fric.sirh.discipline.mapper;

import com.fric.sirh.discipline.dto.ReponseExplicationDTO;
import com.fric.sirh.discipline.model.ReponseExplication;
import com.fric.sirh.discipline.model.DemandeExplication;
import org.springframework.stereotype.Component;

@Component
public class ReponseExplicationMapper {

    public ReponseExplicationDTO toDTO(ReponseExplication entity) {
        if (entity == null) return null;

        return ReponseExplicationDTO.builder()
                .id(entity.getId())
                .demandeExplicationId(entity.getDemandeExplication() != null ? entity.getDemandeExplication().getId() : null)
                .demandeExplicationNumero(entity.getDemandeExplication() != null ? entity.getDemandeExplication().getNumero() : null)
                .contenu(entity.getContenu())
                .piecesJointes(entity.getPiecesJointes())
                .dateReponse(entity.getDateReponse())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReponseExplication toEntity(ReponseExplicationDTO dto, DemandeExplication demande) {
        if (dto == null) return null;

        return ReponseExplication.builder()
                .id(dto.getId())
                .demandeExplication(demande)
                .contenu(dto.getContenu())
                .piecesJointes(dto.getPiecesJointes())
                .dateReponse(dto.getDateReponse())
                .build();
    }
}