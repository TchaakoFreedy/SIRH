package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "Notification")
public class Notification {
    @Id
    private String id;
    private String titre;
    private String message;
    private boolean lu;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;

    @DBRef
    private Notification employee;

    public Notification(){}

    public Notification(String titre, String message, boolean lu,String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt){
        this.titre = titre;
        this.message = message;
        this.lu = lu;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}
