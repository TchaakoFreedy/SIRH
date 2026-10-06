// src/main/java/com/fric/sirh/service/ContractAlertConfigService.java
package com.fric.sirh.service;

import com.fric.sirh.dto.ContractAlertConfigDTO;
import com.fric.sirh.dto.UpdateContractAlertConfigRequest;
import com.fric.sirh.model.ContractAlertConfig;
import com.fric.sirh.repository.ContractAlertConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAlertConfigService {

    private static final String GLOBAL_CONFIG_ID = "global";

    private final ContractAlertConfigRepository repository;

    /**
     * Récupère la configuration courante. Si elle n'existe pas, crée la configuration par défaut.
     */
    public ContractAlertConfig getConfig() {
        Optional<ContractAlertConfig> optional = repository.findById(GLOBAL_CONFIG_ID);
        if (optional.isPresent()) {
            return optional.get();
        }
        return createDefaultConfig();
    }

    /**
     * Récupère la configuration sous forme de DTO.
     */
    public ContractAlertConfigDTO getConfigDTO() {
        ContractAlertConfig config = getConfig();
        return toDTO(config);
    }

    /**
     * Met à jour la configuration à partir d'une requête partielle.
     */
    @Transactional
    public ContractAlertConfigDTO updateConfig(UpdateContractAlertConfigRequest request, String userId) {
        ContractAlertConfig config = getConfig();

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

        ContractAlertConfig saved = repository.save(config);
        log.info("Configuration des alertes mise à jour par {} : {}", userId, saved);
        return toDTO(saved);
    }

    /**
     * Sauvegarde une configuration (utilisée en interne).
     */
    public ContractAlertConfig saveConfig(ContractAlertConfig config) {
        config.setId(GLOBAL_CONFIG_ID);
        return repository.save(config);
    }

    /**
     * Crée la configuration par défaut.
     */
    public ContractAlertConfig createDefaultConfig() {
        ContractAlertConfig config = ContractAlertConfig.builder()
                .id(GLOBAL_CONFIG_ID)
                .enabled(true)
                .daysBefore(14)
                .emailRecipients(Collections.emptyList())
                .emailCc(Collections.emptyList())
                .emailSubject("Alerte expiration de contrat")
                .emailBodyTemplate("Le contrat de l'employe {employeeName} (type {typeContrat}) arrive a expiration le {endDate}. Veuillez prendre les mesures necessaires.")
                .updatedBy("SYSTEM")
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(config);
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
        dto.setUpdatedAt(config.getUpdatedAt() != null
                ? config.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null);
        return dto;
    }
}