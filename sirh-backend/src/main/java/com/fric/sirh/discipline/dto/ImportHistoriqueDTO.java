package com.fric.sirh.discipline.dto;

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
public class ImportHistoriqueDTO {
    private TypeActionHistorique action;
    private LocalDateTime date;
    private String commentaire;
}