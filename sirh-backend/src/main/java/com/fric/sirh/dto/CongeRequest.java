package com.fric.sirh.dto;

import com.fric.sirh.enums.TypeConge;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CongeRequest {
    private String employeeId;
    private TypeConge typeConge;
    private LocalDate jourDebut;
    private LocalDate jourFin;
    private String motif; // Pour les permissions
}