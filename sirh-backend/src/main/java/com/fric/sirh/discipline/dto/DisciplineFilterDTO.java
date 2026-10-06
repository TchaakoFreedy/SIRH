package com.fric.sirh.discipline.dto;

import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineFilterDTO {
    private String employeId;
    private String entrepriseId;
    private String departementId;
    private StatutDemandeExplication statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String auteurId;
}