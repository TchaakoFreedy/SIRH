package com.fric.sirh.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDirectionResponse {

    private long totalEmployees;
    private long totalDepartments;
    private long totalContracts;
    private List<MonthlyEvolution> employeeEvolution;
    private List<MonthlyEvolution> recruitmentEvolution;
    private List<MonthlyEvolution> leaveEvolution;
    private GenderDistribution genderDistribution;
    private List<ContractTypeCount> contractDistribution;
    private List<Alert> alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenderDistribution {
        private long male;
        private long female;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractTypeCount {
        private String type;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEvolution {
        private String month;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String type;
        private String severity;
        private String message;
        private Object details;
        private ContractDetails contractDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractDetails {
        private String employeeName;
        private String contractType;
        private String startDate;
        private String endDate;
        private String status;
        private Long daysRemaining;
    }
}