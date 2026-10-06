package com.fric.sirh.discipline.dto;

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
public class ImportReponseDTO {
    private String contenu;
    private List<String> piecesJointes;
    private LocalDateTime dateReponse;
    private boolean validee;
    private boolean rejetee;
}