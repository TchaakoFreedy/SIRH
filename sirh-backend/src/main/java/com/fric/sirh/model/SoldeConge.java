package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "SoldeConge")
public class SoldeConge {
    @Id
    private String id;
    private LocalDate annee;
    private int jourTotal;
    private int jourUtiliser;
    private int jourRestant;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;

    @DBRef
    private Employee employee;

    public SoldeConge(){}

    public SoldeConge(LocalDate annee, int jourTotal, int jourUtiliser, int jourRestant, String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt){
        this.annee = annee;
        this.jourTotal = jourTotal;
        this.jourRestant = jourRestant;
        this.jourUtiliser = jourRestant;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}
