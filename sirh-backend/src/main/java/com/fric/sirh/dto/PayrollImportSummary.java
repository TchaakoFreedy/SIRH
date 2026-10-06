// com.fric.sirh.dto.PayrollImportSummary
package com.fric.sirh.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PayrollImportSummary {
    private int totalPages;
    private int createdPayrolls;
    private int ocrPages;
    private int textPages;
    private int employeesMatched;
    private int employeesNotMatched;
    private String processingTime;
    private List<String> errors = new ArrayList<>();

    public void incrementTextPages() { this.textPages++; }
    public void incrementOcrPages() { this.ocrPages++; }
    public void incrementEmployeesMatched() { this.employeesMatched++; }
    public void incrementEmployeesNotMatched() { this.employeesNotMatched++; }
}