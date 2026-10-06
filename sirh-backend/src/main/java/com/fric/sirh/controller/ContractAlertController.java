// src/main/java/com/fric/sirh/controller/ContractAlertController.java
package com.fric.sirh.controller;

import com.fric.sirh.service.ContratExpirationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/contracts/alerts")
@RequiredArgsConstructor
public class ContractAlertController {

    private final ContratExpirationScheduler scheduler;

    @PostMapping("/manualCheck")
    @PreAuthorize("hasRole('RH') or hasRole('TOP_MANAGER')")
    public ResponseEntity<Map<String, Object>> manualCheck() {
        Map<String, Object> result = scheduler.manualCheck();
        return ResponseEntity.ok(result);
    }
}