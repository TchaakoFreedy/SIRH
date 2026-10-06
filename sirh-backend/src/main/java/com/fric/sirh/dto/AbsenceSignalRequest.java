package com.fric.sirh.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AbsenceSignalRequest {
    private String employeeId;
    private LocalDate jourDebut;
    private LocalDate jourFin;
    private String motif;
}