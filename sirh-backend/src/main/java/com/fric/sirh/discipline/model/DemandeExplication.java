package com.fric.sirh.discipline.model;

import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "demandes_explication")
public class DemandeExplication {

    @Id
    private String id;

    private String numero; // EXP-2026-0001

    private String objet;

    private String description;

    private String motif;

    @DBRef
    private Employee employeConcerne;

    @DBRef
    private User auteur;

    private String entrepriseId;

    private String departementId;

    private LocalDateTime dateCreation;

    private LocalDateTime dateLimiteReponse;

    private StatutDemandeExplication statut;

    @DBRef
    private ReponseExplication reponse;

    @Builder.Default
    private List<HistoriqueDiscipline> historique = new ArrayList<>();

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}