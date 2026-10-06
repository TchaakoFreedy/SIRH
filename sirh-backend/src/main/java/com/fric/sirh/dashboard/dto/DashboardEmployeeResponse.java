package com.fric.sirh.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DashboardEmployeeResponse {

    // ==================== CHAMPS EXISTANTS ====================
    private int leaveBalance;
    private int takenLeaves;
    private long pendingLeaves;
    private CurrentContract currentContract;
    private List<DocumentInfo> documents;
    private List<Notification> notifications;

    // ==================== NOUVEAUX CHAMPS AVEC @JsonProperty ====================
    @JsonProperty
    private long pendingExplanationRequests;

    @JsonProperty
    private long activeSanctions;

    @JsonProperty
    private long pendingEvaluations;

    @JsonProperty
    @Builder.Default
    private List<MonthlyEvolution> explanationRequestsEvolution = new ArrayList<>();

    @JsonProperty
    @Builder.Default
    private List<SanctionTypeCount> sanctionsByType = new ArrayList<>();

    @JsonProperty
    @Builder.Default
    private List<PerformanceEvolution> performanceEvolution = new ArrayList<>();

    // ==================== CLASSES INTERNES ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentContract {
        private String id;
        private String type;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private String id;
        private String name;
        private String type;
        private List<String> imageUrls;
        private LocalDate uploadDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEvolution {
        private String month;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionTypeCount {
        private String type;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceEvolution {
        private String month;
        private Double averageScore;
    }
}