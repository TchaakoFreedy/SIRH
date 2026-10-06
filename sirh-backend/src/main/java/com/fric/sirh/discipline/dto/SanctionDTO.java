package com.fric.sirh.discipline.dto;

import com.fric.sirh.discipline.enums.StatutSanction;
import com.fric.sirh.discipline.enums.TypeSanction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SanctionDTO {

    private String id;
    private String numero;

    @NotNull(message = "L'employé est obligatoire")
    private String employeId;
    private String employeNom;

    private String demandeExplicationId;
    private String demandeExplicationNumero;

    @NotNull(message = "Le type de sanction est obligatoire")
    private TypeSanction type;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    private Integer duree;

    private StatutSanction statut;

    private String creeParId;
    private String creeParNom;

    private List<HistoriqueDisciplineDTO> historique;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}