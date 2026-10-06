package com.fric.sirh.dashboard.controller;

import com.fric.sirh.dashboard.dto.DashboardDirectionResponse;
import com.fric.sirh.dashboard.dto.DashboardEmployeeResponse;
import com.fric.sirh.dashboard.dto.DashboardManagerResponse;
import com.fric.sirh.dashboard.dto.DashboardRHResponse;
import com.fric.sirh.dashboard.service.DashboardDirectionService;
import com.fric.sirh.dashboard.service.DashboardEmployeeService;
import com.fric.sirh.dashboard.service.DashboardManagerService;
import com.fric.sirh.dashboard.service.DashboardRHService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardRHService dashboardRHService;
    private final DashboardDirectionService dashboardDirectionService;
    private final DashboardManagerService dashboardManagerService;
    private final DashboardEmployeeService dashboardEmployeeService;

    @GetMapping("/rh")
    @PreAuthorize("hasAnyRole('RH', 'TOP_MANAGER')")
    public ResponseEntity<DashboardRHResponse> getRhDashboard() {
        log.info("Récupération du dashboard RH/TOP_MANAGER");
        return ResponseEntity.ok(dashboardRHService.getDashboard());
    }

    @GetMapping("/direction")
    @PreAuthorize("hasRole('DIRECTION')")
    public ResponseEntity<DashboardDirectionResponse> getDirectionDashboard() {
        log.info("Récupération du dashboard DIRECTION");
        return ResponseEntity.ok(dashboardDirectionService.getDashboard());
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DashboardManagerResponse> getManagerDashboard() {
        log.info("Récupération du dashboard MANAGER");
        return ResponseEntity.ok(dashboardManagerService.getDashboard());
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<DashboardEmployeeResponse> getEmployeeDashboard() {
        log.info("Récupération du dashboard EMPLOYEE");
        return ResponseEntity.ok(dashboardEmployeeService.getDashboard());
    }
}