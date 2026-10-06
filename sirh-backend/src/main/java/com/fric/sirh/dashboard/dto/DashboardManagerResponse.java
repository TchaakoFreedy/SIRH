package com.fric.sirh.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardManagerResponse {

    private long teamSize;
    private long employeesAbsentToday;
    private long pendingApprovals;
    private double presenceRate;
    private List<PositionCount> positionsDistribution;
    private List<MonthlyEvolution> leaveEvolution;
    private List<RecentActivity> recentActivities;
    private List<Alert> alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionCount {
        private String positionName;
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
    public static class RecentActivity {
        private LocalDate date;
        private String type;
        private String description;
        private String employeeName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String type;
        private String severity;
        private String message;
        private Long details;
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