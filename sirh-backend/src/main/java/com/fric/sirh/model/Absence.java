package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "Absence")
public class Absence {
    @Id
    private String id;
    private String motif;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private String createdBy;
    private LocalDate createdAt;
    private String updatedBy;
    private LocalDate updatedAt;

    @DBRef
    private Employee employee;

    public Absence(){}

    public Absence(String motif, LocalDate dateDebut, LocalDate dateFin, String statut, String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt){
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
        this.statut = statut;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;

    }
}
