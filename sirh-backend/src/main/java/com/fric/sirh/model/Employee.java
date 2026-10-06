package com.fric.sirh.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.List;

@Data
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String nom;
    private String prenom;

    @Indexed(unique = true)
    @Field("matricule_interne")
    private String matriculeInterne;

    @Indexed(unique = true, sparse = true)
    private String matricule_CNPS;

    private String sexe;

    private Integer nombreEnfantsMoinsDe7Ans;

    private LocalDate date_naissance;

    private String telephone;

    private String numeroContactUrgence;

    private String addresse;

    private LocalDate date_embauche;

    private String statut;

    private String posteId;

    private String departementId;

    private String entrepriseId;

    @Field("salaire_mensuel")
    private Double salaireMensuel;

    @DBRef
    private User user;

    @DBRef(lazy = true)
    private List<Conge> conges;

    private LocalDate createdAt;

    private String createdBy;

    private String updatedBy;

    private LocalDate updatedAt;

    public String getNomComplet() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    public String getCompanyId() {
        return entrepriseId;
    }

    public String getDepartmentId() {
        return departementId;
    }

    public String getPositionId() {
        return posteId;
    }

    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    public Employee() {
    }

    public Employee(
            String id,
            String nom,
            String prenom,
            String matriculeInterne
    ) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.matriculeInterne = matriculeInterne;
    }
}