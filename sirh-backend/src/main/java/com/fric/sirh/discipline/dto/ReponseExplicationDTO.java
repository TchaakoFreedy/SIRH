package com.fric.sirh.discipline.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ReponseExplicationDTO {

    private String id;

    private String demandeExplicationId;
    private String demandeExplicationNumero;

    @NotBlank(message = "Le contenu de la réponse est obligatoire")
    private String contenu;

    private List<String> piecesJointes;

    private LocalDateTime dateReponse;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}