package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinPaieUploadResponse {
    private int totalEmployeesProcessed;
    private int successCount;
    private int failureCount;
    private List<String> errors = new ArrayList<>();
    private String message;
    private String status;

    // Champs additionnels pour l'upload
    private String id;
    private String fileId;
    private int pageCount;
    private List<String> imageIds;
    private boolean employeeFound;
    private List<String> warnings;
    private int totalPages;
    private int createdPayrolls;
    private int ocrPages;
    private int textPages;
    private int employeesMatched;
    private int employeesNotMatched;
    private String processingTime;
}