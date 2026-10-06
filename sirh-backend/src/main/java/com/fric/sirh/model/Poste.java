package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Poste")
public class Poste {
    @Id
    private String id;
    private String code;
    private String libelle;
    private String description;
    private boolean active;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;

    @DBRef
    private Departement departement;

    // Constructeur pour la création
    public Poste(String code, String libelle, String description, String createdBy) {
        this.code = code;
        this.libelle = libelle;
        this.description = description;
        this.active = true;
        this.createdBy = createdBy;
        this.createdAt = LocalDate.now();
    }
}