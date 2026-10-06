// com.fric.sirh.dto.PayrollDto (similaire à BulletinPaieDTO, avec statut et imageIds)
package com.fric.sirh.dto;

import com.fric.sirh.model.PayrollStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PayrollDto {
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
    private List<String> imageIds;
    private String uploadedFileName;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private PayrollStatus status;
    private List<String> importErrors;
}