package com.fric.sirh.discipline.model;

import com.fric.sirh.discipline.enums.TypeActionHistorique;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueDiscipline {
    private String utilisateurId;
    private String utilisateurNom;
    private TypeActionHistorique action;
    private LocalDateTime date;
    private String commentaire;
}