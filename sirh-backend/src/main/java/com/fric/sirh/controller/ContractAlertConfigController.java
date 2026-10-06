// src/main/java/com/fric/sirh/controller/ContractAlertConfigController.java
package com.fric.sirh.controller;

import com.fric.sirh.dto.ContractAlertConfigDTO;
import com.fric.sirh.dto.UpdateContractAlertConfigRequest;
import com.fric.sirh.model.ContractAlertConfig;
import com.fric.sirh.service.ContractAlertConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/contract-alert-configs")
@RequiredArgsConstructor
public class ContractAlertConfigController {

    private final ContractAlertConfigService configService;

    @GetMapping("/global")
    @PreAuthorize("hasRole('RH') or hasRole('TOP_MANAGER')")
    public ResponseEntity<ContractAlertConfigDTO> getConfig() {
        ContractAlertConfig config = configService.getConfig();
        return ResponseEntity.ok(toDTO(config));
    }

    @PutMapping("/global")
    @PreAuthorize("hasRole('RH') or hasRole('TOP_MANAGER')")
    public ResponseEntity<ContractAlertConfigDTO> updateConfig(
            @Valid @RequestBody UpdateContractAlertConfigRequest request) {

        String userId = getCurrentUserId();
        ContractAlertConfig config = configService.getConfig();

        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (request.getDaysBefore() != null) {
            config.setDaysBefore(request.getDaysBefore());
        }
        if (request.getEmailRecipients() != null) {
            config.setEmailRecipients(request.getEmailRecipients());
        }
        if (request.getEmailCc() != null) {
            config.setEmailCc(request.getEmailCc());
        }
        if (request.getEmailSubject() != null) {
            config.setEmailSubject(request.getEmailSubject());
        }
        if (request.getEmailBodyTemplate() != null) {
            config.setEmailBodyTemplate(request.getEmailBodyTemplate());
        }

        config.setUpdatedBy(userId);
        config.setUpdatedAt(LocalDateTime.now());

        ContractAlertConfig updated = configService.saveConfig(config);
        log.info("Configuration des alertes mise à jour par {}", userId);
        return ResponseEntity.ok(toDTO(updated));
    }

    private ContractAlertConfigDTO toDTO(ContractAlertConfig config) {
        ContractAlertConfigDTO dto = new ContractAlertConfigDTO();
        dto.setId(config.getId());
        dto.setEnabled(config.isEnabled());
        dto.setDaysBefore(config.getDaysBefore());
        dto.setEmailRecipients(config.getEmailRecipients());
        dto.setEmailCc(config.getEmailCc());
        dto.setEmailSubject(config.getEmailSubject());
        dto.setEmailBodyTemplate(config.getEmailBodyTemplate());
        dto.setUpdatedBy(config.getUpdatedBy());
        dto.setUpdatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);
        return dto;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "UNKNOWN";
    }
}