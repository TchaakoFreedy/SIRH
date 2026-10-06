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
public class DashboardRHResponse {

    // Statistiques globales
    private long totalEmployees;
    private long totalDepartments;
    private long totalPositions;
    private long activeContracts;
    private long employeesOnLeaveToday;
    private long pendingLeaveRequests;
    private long contractsExpiringSoon;
    private long missingDocuments;

    // Graphiques
    private GenderDistribution genderDistribution;
    private List<DepartmentEmployeeCount> employeesByDepartment;
    private List<ContractTypeCount> contractDistribution;
    private List<MonthlyEvolution> recruitmentEvolution;
    private List<MonthlyEvolution> leaveEvolution;

    // Activités et alertes
    private List<RecentActivity> recentActivities;
    private List<Alert> alerts;

    // ============================================================
    // CLASSES INTERNES
    // ============================================================

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
    public static class DepartmentEmployeeCount {
        private String departmentName;
        private long count;
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
        private String month;   // "Jan", "Fév", ...
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private LocalDate date;
        private String type;    // "CONGÉ", "CONTRAT", "EMBAUCHE", ...
        private String description;
        private String employeeName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String type;        // "CONTRAT_EXPIRANT", "DOCUMENT_MANQUANT", "CONGES_EN_ATTENTE"
        private String severity;    // "INFO", "WARNING", "CRITICAL"
        private String message;

        // Pour les alertes génériques (ex: nombre de congés en attente)
        private Long details;

        // Pour les alertes détaillées de contrat
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