// src/main/java/com/fric/sirh/dto/RenouvellementContratRequest.java
package com.fric.sirh.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class RenouvellementContratRequest {

    @NotBlank(message = "L'ID du contrat est obligatoire")
    private String contratId;

    @NotNull(message = "La nouvelle date de fin est obligatoire")
    @Future(message = "La date de fin doit être future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nouvelleDateFin;

    private String nouveauTypeContrat;

    private List<String> imageUrls;

}