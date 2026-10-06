package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "Departement")
public class Departement {
    @Id
    private String id;
    private String name;
    private String statut;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;
    private String entrepriseId;



    public Departement(){}

    public Departement(String entrepriseId, String name,String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt, String statut){
        this.name = name;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.statut = statut;
        this.entrepriseId = entrepriseId;
    }
}
