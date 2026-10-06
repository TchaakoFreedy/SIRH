package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "configurations_conges")
public class ConfigurationConge {

    @Id
    private String id;

    // Type: "GLOBALE", "GENRE" ou "INDIVIDUELLE"
    private String type;

    // Si type = "INDIVIDUELLE", l'ID de l'employé concerné
    private String employeeId;

    // Si type = "GENRE", le genre ("FEMME" ou "HOMME")
    private String genre;

    // Nom de la configuration (pour affichage)
    private String nom;

    // Nombre de jours de base (frontend envoie joursDeBase)
    private int joursDeBase;

    // Activer le bonus enfant
    private Boolean bonusEnfantActif;

    // Jours supplémentaires par enfant
    private int joursParEnfant;

    // Âge maximum des enfants pour bénéficier du bonus
    private int ageMaxEnfant;

    // Année de validité (null = toutes années)
    private Integer annee;

    // Audit
    private LocalDate createdAt;
    private String createdBy;
    private LocalDate updatedAt;
    private String updatedBy;
}