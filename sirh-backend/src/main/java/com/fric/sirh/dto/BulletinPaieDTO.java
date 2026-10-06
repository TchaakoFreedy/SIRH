package com.fric.sirh.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BulletinPaieDTO {
    private String id;
    private String employeeId;
    private String employeeMatricule;
    private String employeeFullName;
    private int month;
    private int year;
    private String period;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal deductions;
    private String pdfFileUrl;
    private List<String> imageUrls;  // UNIQUE liste des URLs
    private String uploadedFileName;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String status;
    private List<String> importErrors;
}