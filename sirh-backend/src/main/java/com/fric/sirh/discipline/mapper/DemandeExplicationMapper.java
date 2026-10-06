package com.fric.sirh.discipline.mapper;

import com.fric.sirh.discipline.dto.DemandeExplicationDTO;
import com.fric.sirh.discipline.dto.HistoriqueDisciplineDTO;
import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.model.HistoriqueDiscipline;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DemandeExplicationMapper {

    public DemandeExplicationDTO toDTO(DemandeExplication entity) {
        if (entity == null) return null;

        DemandeExplicationDTO dto = new DemandeExplicationDTO();
        dto.setId(entity.getId());
        dto.setNumero(entity.getNumero());
        dto.setObjet(entity.getObjet());
        dto.setDescription(entity.getDescription());
        dto.setMotif(entity.getMotif());
        dto.setEmployeConcerneId(entity.getEmployeConcerne() != null ? entity.getEmployeConcerne().getId() : null);
        dto.setEmployeConcerneNom(entity.getEmployeConcerne() != null ? entity.getEmployeConcerne().getNomComplet() : null);
        dto.setAuteurId(entity.getAuteur() != null ? entity.getAuteur().getId() : null);
        dto.setAuteurNom(entity.getAuteur() != null ? entity.getAuteur().getFullName() : null);
        dto.setEntrepriseId(entity.getEntrepriseId());
        dto.setDepartementId(entity.getDepartementId());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateLimiteReponse(entity.getDateLimiteReponse());
        dto.setStatut(entity.getStatut());

        if (entity.getHistorique() != null) {
            dto.setHistorique(entity.getHistorique().stream()
                    .map(this::toHistoriqueDTO)
                    .collect(Collectors.toList()));
        }

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public HistoriqueDisciplineDTO toHistoriqueDTO(HistoriqueDiscipline historique) {
        if (historique == null) return null;

        HistoriqueDisciplineDTO dto = new HistoriqueDisciplineDTO();
        dto.setUtilisateurId(historique.getUtilisateurId());
        dto.setUtilisateurNom(historique.getUtilisateurNom());
        dto.setAction(historique.getAction());
        dto.setDate(historique.getDate());
        dto.setCommentaire(historique.getCommentaire());
        return dto;
    }
}