package com.fric.sirh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateContractAlertConfigRequest {

    @NotBlank(message = "L'identifiant de l'entreprise est requis")
    private String companyId;

    private Boolean enabled;

    @Positive(message = "Le nombre de jours doit être positif")
    private Integer daysBefore;

    private List<String> emailRecipients;

    private List<String> emailCc;

    private String emailSubject;

    private String emailBodyTemplate;

    private Boolean replaceActive;
}