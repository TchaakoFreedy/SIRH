package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "Entreprise")

public class Entreprise {
    @Id
    private String id;
    private String name;
    private String siege;
    private String adresse;
    private String telephone;
    private String email;
    private String statut;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;

    public Entreprise() {}

    public Entreprise(String name, String siege, String adresse, String telephone, String email, String statut,String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt) {
        this.name = name;
        this.siege = siege;
        this.adresse = adresse;
        this.telephone = telephone;
        this.email = email;
        this.statut = statut;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}
