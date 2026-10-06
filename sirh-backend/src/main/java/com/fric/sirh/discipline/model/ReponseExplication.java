package com.fric.sirh.discipline.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reponses_explication")
public class ReponseExplication {

    @Id
    private String id;

    @DBRef
    private DemandeExplication demandeExplication;

    private String contenu;

    private List<String> piecesJointes; // URLs ou IDs des fichiers

    private LocalDateTime dateReponse;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}