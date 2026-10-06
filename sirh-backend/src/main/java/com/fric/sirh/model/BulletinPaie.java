package com.fric.sirh.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "BulletinPaie")
public class BulletinPaie {
    @Id
    private String id;

    // Champs extraits du PDF
    private String employeeMatricule;
    private String employeeFullName;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal deductions;
    private int month;
    private int year;
    private String period;

    // URLs des fichiers stockés sur le système de fichiers
    private String pdfFileUrl;
    private String uploadedFileName;

    // UNIQUE liste des URLs des images PNG
    private List<String> imageUrls = new ArrayList<>();

    // Statut
    private BulletinStatus status = BulletinStatus.PROCESSING;
    private List<String> importErrors = new ArrayList<>();

    // Relation avec l'employé
    @DBRef
    private Employee employee;

    // Métadonnées
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}