package com.fric.sirh.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conges")
public class Conge {

    @Id
    private String id;

    private TypeConge typeConge;

    private Integer nbJour;

    private LocalDate jourDebut;

    private LocalDate jourFin;

    private StatutConge statut;

    // vraie relation avec Employee
    @DBRef
    private Employee employee;

    private String managerId;

    private String commentaireManager;

    private LocalDate dateValidation;

    private String createdBy;

    private LocalDate createdAt;

    private String updatedBy;

    private LocalDate updatedAt;

    // Nouveau champ pour l'année de référence
    private Integer annee;
}