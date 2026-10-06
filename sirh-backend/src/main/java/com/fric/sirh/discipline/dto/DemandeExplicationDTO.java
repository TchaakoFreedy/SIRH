package com.fric.sirh.discipline.dto;

import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class DemandeExplicationDTO {

    private String id;
    private String numero;

    @NotBlank(message = "L'objet est obligatoire")
    @Size(max = 255, message = "L'objet ne doit pas dépasser 255 caractères")
    private String objet;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;

    @NotNull(message = "L'employé concerné est obligatoire")
    private String employeConcerneId;
    private String employeConcerneNom;

    private String auteurId;
    private String auteurNom;

    private String entrepriseId;
    private String entrepriseNom;

    private String departementId;
    private String departementNom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateLimiteReponse;

    private StatutDemandeExplication statut;

    private ReponseExplicationDTO reponse;

    private List<HistoriqueDisciplineDTO> historique;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}