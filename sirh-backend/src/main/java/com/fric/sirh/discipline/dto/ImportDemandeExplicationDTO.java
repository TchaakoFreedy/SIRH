package com.fric.sirh.discipline.dto;

import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportDemandeExplicationDTO {
    private String numeroOriginal;
    private String objet;
    private String description;
    private String motif;
    private String employeConcerneIdentifier;
    private String auteurIdentifier;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLimiteReponse;
    private StatutDemandeExplication statut;
    private ImportReponseDTO reponse;
    private List<ImportHistoriqueDTO> historique;
}