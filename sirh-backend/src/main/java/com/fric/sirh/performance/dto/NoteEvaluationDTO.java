package com.fric.sirh.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEvaluationDTO {

    @NotNull(message = "Le critère est obligatoire")
    private String critereId;

    private String critereNom;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 0, message = "La note doit être supérieure ou égale à 0")
    @Max(value = 100, message = "La note ne peut pas dépasser 100")
    private Double note;

    private Integer coefficient;
    private Double scorePondere;
}