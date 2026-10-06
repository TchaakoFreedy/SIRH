package com.fric.sirh.dto.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeHistoryEvent {
    private LocalDateTime date;
    private String type;          // HIRING, CONTRACT_START, CONTRACT_END, CONTRACT_TERMINATION, LEAVE, ABSENCE, DOCUMENT, PAYSLIP, PERFORMANCE, LEAVE_BALANCE, STATUS_CHANGE
    private String description;
    private Map<String, Object> details;
}