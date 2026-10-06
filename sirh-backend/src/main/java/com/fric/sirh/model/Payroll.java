// com.fric.sirh.model.Payroll
package com.fric.sirh.model;

import lombok.*;
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
@Document(collection = "payrolls")
public class Payroll {
    @Id
    private String id;

    // Informations extraites
    private String employeeMatricule;
    private String employeeFullName;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal deductions;
    private int month;
    private int year;
    private String period; // "MM/YYYY"

    // Stockage
    private String originalPdfId;      // ID GridFS du PDF original (optionnel)
    private List<String> imageIds = new ArrayList<>(); // IDs GridFS des pages PNG
    private String uploadedFileName;

    // Relations
    @DBRef
    private Employee employee;

    // Métadonnées
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // Statut et erreurs
    private PayrollStatus status = PayrollStatus.PROCESSING;
    private List<String> importErrors = new ArrayList<>();
}