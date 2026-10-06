package com.fric.sirh.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeHistoryResponse {
    private String employeeId;
    private String employeeName;
    private List<EmployeeHistoryEvent> events;
}