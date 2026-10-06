package com.fric.sirh.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "documents")
public class Documents {

    @Id
    private String id;

    private String name;
    private String typeDocument;

    private List<String> imageUrls = new ArrayList<>();

    private LocalDate dateUpload;

    @DBRef
    private Contrat contrat;

    @DBRef
    @NotNull(message = "Un document doit être rattaché à un employé")
    private Employee employee;

    private LocalDate createdAt;
    private String createdBy;
    private LocalDate updatedAt;
    private String updatedBy;
}